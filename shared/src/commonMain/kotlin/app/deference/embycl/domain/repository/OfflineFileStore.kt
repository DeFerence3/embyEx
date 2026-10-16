package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.download.OfflineAssetKind

enum class OfflineStorageFailure { UNAVAILABLE, FULL, BUSY, INVALID_PATH, SIZE_MISMATCH, REMOVAL_PENDING, UNSUPPORTED }

/** Safe reason for callers; never persist an exception message as download metadata. */
class OfflineStorageException(val reason: OfflineStorageFailure, cause: Throwable? = null) :
    Exception("Offline storage: $reason", cause)

data class OfflineFile(val relativePath: String, val absolutePath: String, val sizeBytes: Long)

/** Names come from this recipe, never from a URL, title or remote filename. */
data class OfflineAssetTarget(val kind: OfflineAssetKind, val extension: String, val streamIndex: Int? = null) {
    init {
        if (kind == OfflineAssetKind.SUBTITLE) {
            require(streamIndex != null && streamIndex >= 0)
            require(extension in setOf("srt", "vtt", "ass", "ssa"))
        } else {
            require(streamIndex == null)
            require(extension in setOf("jpg", "png", "webp"))
        }
    }
    val id: String get() = if (kind == OfflineAssetKind.SUBTITLE) "subtitle-$streamIndex" else kind.name.lowercase()
    val fileName: String get() = "$id.$extension"
    val maxBytes: Long get() = if (kind == OfflineAssetKind.SUBTITLE) 8L * 1024 * 1024 else 32L * 1024 * 1024
}

interface OfflineFileWriter {
    val bytesWritten: Long
    /** At most 1 MiB per call. Counts include an existing media partial when resuming. */
    suspend fun write(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size - offset)
}

interface OfflineFileStore {
    /** One queue executor across processes; acquire before per-download leases. */
    suspend fun <T> withQueueLease(block: suspend () -> T): T
    /** Separate cross-process owner for remote progress reconciliation; never blocks media transfers. */
    suspend fun <T> withSyncLease(block: suspend () -> T): T
    /** A per-download OS lock covers both file work and the caller's database commits.
     * Use the lease and its writer sequentially inside the block; neither may escape.
     * Contention fails BUSY rather than blocking a worker. All file-affecting state
     * mutations must participate in this lease, including pause/cancel/removal.
     */
    suspend fun <T> withDownload(downloadId: String, block: suspend (DownloadFiles) -> T): T
    suspend fun downloadIds(): List<String>
}

interface DownloadFiles {
    val downloadId: String
    suspend fun partialSize(): Long
    /** Exact offset required; never truncates or silently appends at a different offset. */
    suspend fun writeMedia(expectedOffset: Long, totalBytes: Long?, block: suspend (OfflineFileWriter) -> Unit): Long
    /** Only call for an incomplete job after deciding that its old bytes cannot be resumed. */
    suspend fun discardMedia(container: String)
    /** Caller must have persisted VERIFYING after successful EOF and source validation. */
    suspend fun finalizeMedia(container: String, verifiedBytes: Long): OfflineFile
    suspend fun writeAsset(target: OfflineAssetTarget, expectedBytes: Long?, block: suspend (OfflineFileWriter) -> Unit): OfflineFile
    suspend fun discardAsset(target: OfflineAssetTarget)
    /** Returns null for absent/wrong-size files. I/O failures are not reported as missing. */
    suspend fun findFile(relativePath: String, expectedBytes: Long? = null): OfflineFile?
    suspend fun discardAssetPartials()
    suspend fun removalPending(): Boolean
    suspend fun beginRemoval()
    /** Keeps the removal marker so interrupted deletion can be retried. Also used for cancellation. */
    suspend fun deletePayloads()
    /** Only after the database download/asset rows have been removed. */
    suspend fun finishRemoval()
}
