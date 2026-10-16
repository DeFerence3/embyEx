package app.deference.embycl.data.local.database

import app.deference.embycl.domain.repository.DownloadFiles
import app.deference.embycl.domain.repository.OfflineAssetTarget
import app.deference.embycl.domain.repository.OfflineFile
import app.deference.embycl.domain.repository.OfflineFileStore
import app.deference.embycl.domain.repository.OfflineFileWriter
import app.deference.embycl.domain.repository.OfflineStorageException
import app.deference.embycl.domain.repository.OfflineStorageFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException

/** Same implementation on Android and desktop. Does not follow links or recursively delete. */
internal class JavaOfflineFileStore(
    private val configuredRoot: File,
    private val ops: OfflineFileOps,
    private val freeBytes: (File) -> Long = { it.usableSpace },
    private val reserveBytes: Long = 16L * 1024 * 1024,
) : OfflineFileStore {
	
    init { require(reserveBytes >= 0) }
    private val idPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private val payloadPattern = Regex("(?:media\\.[a-z0-9]{1,12}|(?:poster|backdrop|logo)\\.(?:jpg|png|webp)|subtitle-[0-9]+\\.(?:srt|vtt|ass|ssa))(?:\\.part)?")

    private fun validateId(id: String) { if (!idPattern.matches(id)) fail(OfflineStorageFailure.INVALID_PATH) }
    private fun fail(reason: OfflineStorageFailure): Nothing = throw OfflineStorageException(reason)
    private fun <T> io(block: () -> T): T = try { block() } catch (e: IOException) {
        throw OfflineStorageException(OfflineStorageFailure.UNAVAILABLE, e)
    } catch (e: SecurityException) { throw OfflineStorageException(OfflineStorageFailure.UNAVAILABLE, e) }

    private fun directory(file: File, create: Boolean): File {
        val entry = ops.stat(file)
        if (entry == null && create) {
            if (file.mkdir()) {
                ops.syncDirectory(file.parentFile ?: fail(OfflineStorageFailure.INVALID_PATH))
            } else if (ops.stat(file)?.directory != true) fail(OfflineStorageFailure.UNAVAILABLE)
        } else if (entry?.directory != true) fail(OfflineStorageFailure.UNAVAILABLE)
        // stat rejects links (including Windows junctions); canonical comparison catches aliases.
        if (file.canonicalFile != file.absoluteFile) fail(OfflineStorageFailure.INVALID_PATH)
        return file
    }

    private fun root(): File = io {
        // The configured application-data parent is trusted; descendants are strictly checked.
        val parent = configuredRoot.absoluteFile.parentFile ?: fail(OfflineStorageFailure.INVALID_PATH)
        if (!parent.isDirectory && !parent.mkdirs()) fail(OfflineStorageFailure.UNAVAILABLE)
        directory(File(parent.canonicalFile, configuredRoot.name), create = true)
    }

    private fun checked(root: File, parent: File, name: String): File {
        directory(root, create = false)
        directory(parent, create = false)
        val file = File(parent, name)
        if (file.canonicalFile != file.absoluteFile || !file.absolutePath.startsWith(root.absolutePath + File.separator))
            fail(OfflineStorageFailure.INVALID_PATH)
        val entry = ops.stat(file)
        if (entry != null && !entry.regular) fail(OfflineStorageFailure.INVALID_PATH)
        return file
    }

    override suspend fun downloadIds(): List<String> = withContext(Dispatchers.IO) { io {
        val root = root()
        (root.listFiles() ?: fail(OfflineStorageFailure.UNAVAILABLE)).filter { idPattern.matches(it.name) }.map {
            directory(it, create = false)
            it.name
        }
    } }

    override suspend fun <T> withDownload(downloadId: String, block: suspend (DownloadFiles) -> T): T = withContext(Dispatchers.IO) {
        validateId(downloadId)
        locked(downloadId) { root ->
            val directory = io { directory(File(root, downloadId), create = true) }
            val lease = Lease(root, directory, downloadId)
            try { block(lease) } finally { lease.active = false }
        }
    }

    override suspend fun <T> withQueueLease(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        locked("queue") { block() }
    }
    override suspend fun <T> withSyncLease(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        locked("sync") { block() }
    }

    private suspend fun <T> locked(name: String, block: suspend (File) -> T): T {
        val root = root()
        val locks = io { directory(File(root, ".locks"), create = true) }
        val lockFile = io { checked(root, locks, "$name.lock") }
        val handle = io { RandomAccessFile(lockFile, "rw") }
        return handle.use {
            val lock = try { io { handle.channel.tryLock() } } catch (_: OverlappingFileLockException) { null }
                ?: fail(OfflineStorageFailure.BUSY)
            try {
                block(root)
            } finally { lock.release() }
        }
    }

    private inner class Lease(val root: File, val directory: File, override val downloadId: String) : DownloadFiles {
        var active = true
        private var writing = false
        private fun path(name: String): File {
            check(active) { "Download file lease has ended." }
            return checked(root, directory, name)
        }
        private fun available() { if (ops.stat(path(".remove")) != null) fail(OfflineStorageFailure.REMOVAL_PENDING) }
        private fun mediaName(container: String): String {
            val extension = container.lowercase()
            if (extension == "part" || !extension.matches(Regex("[a-z0-9]{1,12}"))) fail(OfflineStorageFailure.INVALID_PATH)
            return "media.$extension"
        }
        private fun space(required: Long) {
            val free = freeBytes(directory)
            if (free < reserveBytes || required > free - reserveBytes) fail(OfflineStorageFailure.FULL)
        }
        override suspend fun partialSize(): Long = io { available(); ops.stat(path("media.part"))?.size ?: 0 }

        private suspend fun write(name: String, offset: Long, total: Long?, limit: Long?, block: suspend (OfflineFileWriter) -> Unit): Long {
            require(offset >= 0 && (total == null || total > 0) && (total == null || offset <= total))
            check(!writing) { "Only one writer may use a download lease." }
            val file = io {
                available()
                val file = path(name)
                if ((ops.stat(file)?.size ?: 0) != offset) fail(OfflineStorageFailure.SIZE_MISMATCH)
                space((total ?: offset) - offset)
                file
            }
            writing = true
            try {
                val handle = io { RandomAccessFile(file, "rw") }
                return handle.use {
                    io { if (handle.length() != offset) fail(OfflineStorageFailure.SIZE_MISMATCH); handle.seek(offset) }
                    var open = true
                    val writer = object : OfflineFileWriter {
                        override var bytesWritten = offset
                            private set
                        override suspend fun write(bytes: ByteArray, offset: Int, count: Int) {
                            currentCoroutineContext().ensureActive()
                            check(open && active) { "File writer has closed." }
                            require(offset >= 0 && count in 0..1_048_576 && offset <= bytes.size - count)
                            io {
                                available()
                                if (bytesWritten > Long.MAX_VALUE - count || (total != null && count > total - bytesWritten) ||
                                    (limit != null && count > limit - bytesWritten)) fail(OfflineStorageFailure.SIZE_MISMATCH)
                                space(count.toLong())
                                try { handle.write(bytes, offset, count) } catch (e: IOException) {
                                    // Space can run out between preflight and the actual write.
                                    if (freeBytes(directory) <= reserveBytes + count) throw OfflineStorageException(OfflineStorageFailure.FULL, e)
                                    throw e
                                }
                                bytesWritten += count
                            }
                        }
                    }
                    try { block(writer); currentCoroutineContext().ensureActive(); io { handle.fd.sync() }; writer.bytesWritten }
                    finally { open = false }
                }
            } finally { writing = false }
        }

        override suspend fun writeMedia(expectedOffset: Long, totalBytes: Long?, block: suspend (OfflineFileWriter) -> Unit): Long =
            write("media.part", expectedOffset, totalBytes, null, block)

        private fun remove(name: String) {
            val file = path(name)
            if (ops.stat(file) != null) ops.delete(file)
        }
        override suspend fun discardMedia(container: String) = io {
            check(!writing); available()
            val final = mediaName(container)
            path("media.part"); path(final)
            remove("media.part"); remove(final); ops.syncDirectory(directory)
        }

        private fun inspect(name: String, expected: Long?): OfflineFile? {
            val file = path(name)
            val size = ops.stat(file)?.size ?: return null
            if (size <= 0 || (expected != null && size != expected)) return null
            return OfflineFile("$downloadId/$name", file.absolutePath, size)
        }
        private fun publish(part: String, final: String, expected: Long): OfflineFile {
            require(expected > 0)
            check(!writing)
            available()
            val source = path(part)
            val destination = path(final)
            if (ops.stat(destination) != null) {
                // Recovery after rename but before the database commit: never overwrite.
                if (ops.stat(source) != null) fail(OfflineStorageFailure.SIZE_MISMATCH)
                val file = inspect(final, expected) ?: fail(OfflineStorageFailure.SIZE_MISMATCH)
                RandomAccessFile(destination, "rw").use { it.fd.sync() }
                ops.syncDirectory(directory)
                return file
            }
            if (ops.stat(source)?.size != expected) fail(OfflineStorageFailure.SIZE_MISMATCH)
            RandomAccessFile(source, "rw").use { it.fd.sync() }
            ops.moveAtomic(source, destination)
            ops.syncDirectory(directory)
            return inspect(final, expected) ?: fail(OfflineStorageFailure.SIZE_MISMATCH)
        }
        override suspend fun finalizeMedia(container: String, verifiedBytes: Long): OfflineFile = io {
            publish("media.part", mediaName(container), verifiedBytes)
        }
        override suspend fun writeAsset(target: OfflineAssetTarget, expectedBytes: Long?, block: suspend (OfflineFileWriter) -> Unit): OfflineFile {
            require(expectedBytes == null || expectedBytes in 1..target.maxBytes)
            io {
                check(!writing)
                available()
                if (ops.stat(path(target.fileName)) != null) fail(OfflineStorageFailure.SIZE_MISMATCH)
                remove("${target.fileName}.part")
            }
            val size = write("${target.fileName}.part", 0, expectedBytes, target.maxBytes, block)
            if (size == 0L || (expectedBytes != null && size != expectedBytes)) fail(OfflineStorageFailure.SIZE_MISMATCH)
            return io { publish("${target.fileName}.part", target.fileName, size) }
        }
        override suspend fun discardAsset(target: OfflineAssetTarget) = io {
            check(!writing); available(); remove(target.fileName); remove("${target.fileName}.part"); ops.syncDirectory(directory)
        }
        override suspend fun findFile(relativePath: String, expectedBytes: Long?): OfflineFile? = io {
            available()
            require(expectedBytes == null || expectedBytes > 0)
            val name = relativePath.removePrefix("$downloadId/")
            if (name == relativePath || !payloadPattern.matches(name) || name.endsWith(".part")) fail(OfflineStorageFailure.INVALID_PATH)
            inspect(name, expectedBytes)
        }
        private fun names(): List<String> = (directory.list() ?: fail(OfflineStorageFailure.UNAVAILABLE)).toList()
        override suspend fun discardAssetPartials() = io {
            check(!writing); available()
            names().filter { it != "media.part" && payloadPattern.matches(it) && it.endsWith(".part") }.forEach(::remove)
            ops.syncDirectory(directory)
        }
        override suspend fun removalPending(): Boolean = io { ops.stat(path(".remove")) != null }
        override suspend fun beginRemoval() = io {
            check(!writing)
            RandomAccessFile(path(".remove"), "rw").use { it.fd.sync() }
            ops.syncDirectory(directory)
        }
        override suspend fun deletePayloads() = io {
            check(!writing)
            // Validate every entry before deleting any; never descend into a directory/link.
            val names = names().filter { it != ".remove" }
            names.forEach { if (it != "media.part" && !payloadPattern.matches(it)) fail(OfflineStorageFailure.INVALID_PATH); path(it) }
            names.forEach(::remove)
            ops.syncDirectory(directory)
        }
        override suspend fun finishRemoval() = io {
            check(!writing)
            check(names().all { it == ".remove" }) { "Payloads must be deleted before finishing removal." }
            remove(".remove")
            ops.delete(directory)
            ops.syncDirectory(root)
        }
    }
}
