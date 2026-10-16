package app.deference.embycl.data.local.database

import app.deference.embycl.domain.repository.*
import org.koin.core.annotation.Single
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.attribute.BasicFileAttributes

@Single(binds = [OfflineFileStore::class])
class PlatformOfflineFileStore : OfflineFileStore by JavaOfflineFileStore(
    File(offlineDataDirectory(), "downloads"), JvmOfflineFileOps,
)

internal object JvmOfflineFileOps : OfflineFileOps {
    override fun stat(file: File): FileEntry? {
        val attrs = try { Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, NOFOLLOW_LINKS) }
        catch (_: NoSuchFileException) { return null }
        if (attrs.isSymbolicLink || attrs.isOther) throw OfflineStorageException(OfflineStorageFailure.INVALID_PATH)
        return FileEntry(attrs.isDirectory, attrs.isRegularFile, attrs.size())
    }
    override fun moveAtomic(source: File, destination: File) {
        Files.move(source.toPath(), destination.toPath(), ATOMIC_MOVE)
    }
    override fun syncDirectory(directory: File) {
        if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            FileChannel.open(directory.toPath(), READ).use { it.force(true) }
        }
    }
}
