package app.deference.embycl.domain.model.download

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    @SerialName("queued") QUEUED,
    @SerialName("downloading") DOWNLOADING,
    @SerialName("paused") PAUSED,
    @SerialName("waiting_for_network") WAITING_FOR_NETWORK,
    @SerialName("waiting_for_authentication") WAITING_FOR_AUTHENTICATION,
    @SerialName("verifying") VERIFYING,
    @SerialName("completed") COMPLETED,
    @SerialName("failed") FAILED,
    @SerialName("canceled") CANCELED,
}



/**
 * Immutable lifecycle state shared by persistence, the worker, and UI.
 *
 * This class performs no I/O. The repository must serialize mutations per key;
 * workers must ignore callbacks from canceled/superseded attempts. Disk recovery
 * and source validation must happen before resuming at a persisted byte offset.
 */
@Serializable
@ConsistentCopyVisibility
data class DownloadTask private constructor(
    val key: DownloadKey,
    val status: DownloadStatus,
    val progress: DownloadProgress,
    val failure: DownloadFailureReason? = null,
) {
    init {
        require((status == DownloadStatus.FAILED) == (failure != null)) {
            "Only failed tasks must carry a failure reason."
        }
        if (status == DownloadStatus.VERIFYING || status == DownloadStatus.COMPLETED) {
            require(progress.canVerify) { "Empty or incomplete media cannot be verified." }
        }
        if (status == DownloadStatus.COMPLETED) {
            require(progress.totalBytes == progress.downloadedBytes) {
                "Completed media must have a known final size."
            }
        }
    }

    fun start(): DownloadTask {
        requireStatus(DownloadStatus.QUEUED)
        return copy(status = DownloadStatus.DOWNLOADING)
    }

    /** Persist only byte counts written to the partial file, not merely received. */
    fun updateProgress(
        downloadedBytes: Long,
        totalBytes: Long? = progress.totalBytes,
    ): DownloadTask {
        requireStatus(DownloadStatus.DOWNLOADING)
        require(downloadedBytes >= progress.downloadedBytes) {
            "Progress cannot move backwards without restarting the transfer."
        }
        require(progress.totalBytes == null || totalBytes == progress.totalBytes) {
            "Expected size cannot change without restarting the transfer."
        }
        return copy(progress = DownloadProgress(downloadedBytes, totalBytes))
    }

    /** Call after safely discarding old partial bytes when range resume is unsafe. */
    fun restartFromBeginning(totalBytes: Long? = null): DownloadTask {
        requireStatus(DownloadStatus.DOWNLOADING)
        return copy(progress = DownloadProgress(totalBytes = totalBytes))
    }

    fun pause(): DownloadTask {
        requireStatus(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.WAITING_FOR_NETWORK,
            DownloadStatus.WAITING_FOR_AUTHENTICATION,
        )
        return copy(status = DownloadStatus.PAUSED)
    }

    fun waitForNetwork(): DownloadTask {
        requireStatus(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)
        return copy(status = DownloadStatus.WAITING_FOR_NETWORK)
    }

    fun waitForAuthentication(): DownloadTask {
        requireStatus(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING)
        return copy(status = DownloadStatus.WAITING_FOR_AUTHENTICATION)
    }

    /** Explicit user resume/retry, or worker retry after the recorded wait is resolved. */
    fun retry(): DownloadTask {
        requireStatus(
            DownloadStatus.PAUSED,
            DownloadStatus.WAITING_FOR_NETWORK,
            DownloadStatus.WAITING_FOR_AUTHENTICATION,
            DownloadStatus.FAILED,
        )
        return copy(status = DownloadStatus.QUEUED, failure = null)
    }

    /** The transfer must have reached a successful EOF before calling this. */
    fun beginVerification(): DownloadTask {
        requireStatus(DownloadStatus.DOWNLOADING)
        return copy(status = DownloadStatus.VERIFYING)
    }

    /**
     * Call only after integrity checks and durable file finalization succeed.
     * The model cannot establish file existence or integrity from byte counts.
     */
    fun completeAfterVerification(): DownloadTask {
        requireStatus(DownloadStatus.VERIFYING)
        return copy(
            status = DownloadStatus.COMPLETED,
            progress = progress.copy(totalBytes = progress.downloadedBytes),
        )
    }

    fun fail(reason: DownloadFailureReason): DownloadTask {
        requireStatus(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING)
        return copy(status = DownloadStatus.FAILED, failure = reason)
    }

    /** Cancellation requests partial-file cleanup; it never deletes a completed download. */
    fun cancel(): DownloadTask {
        check(status != DownloadStatus.COMPLETED) { "Use local removal for a completed download." }
        return copy(status = DownloadStatus.CANCELED, failure = null)
    }

    /**
     * Requeue an interrupted writer without auto-resuming user-paused jobs.
     * VERIFYING stays VERIFYING: recovery must inspect partial/final files and
     * repeat finalization rather than assume completion or redownload blindly.
     */
    fun recoverAfterProcessRestart(): DownloadTask =
        if (status == DownloadStatus.DOWNLOADING) copy(status = DownloadStatus.QUEUED) else this

    private fun requireStatus(vararg allowed: DownloadStatus) {
        check(status in allowed) { "Operation is not allowed while a download is $status." }
    }

    companion object {
        fun queued(key: DownloadKey, totalBytes: Long? = null): DownloadTask = DownloadTask(
            key = key,
            status = DownloadStatus.QUEUED,
            progress = DownloadProgress(totalBytes = totalBytes),
        )
    }
}
