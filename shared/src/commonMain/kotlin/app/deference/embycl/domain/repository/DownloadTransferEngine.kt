package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.download.*

sealed interface DownloadTransferResult {
    /** Completed, failed, or waiting; the persisted lifecycle is authoritative. */
    data class Saved(val download: StoredDownload) : DownloadTransferResult
    data class Skipped(val reason: TransferSkipReason, val storageFailure: OfflineStorageFailure? = null) : DownloadTransferResult
}

enum class TransferSkipReason { STALE, NOT_QUEUED, STORAGE }

interface DownloadTransferEngine {
    /** One explicitly queued job, using a fresh account/connection snapshot. Cancellation
     * propagates after saving written progress; C8 then applies pause/cancel under ownership.
     * Never auto-starts paused, completed, failed or waiting jobs.
     */
    suspend fun transfer(connection: DownloadConnection, key: DownloadKey, expected: DownloadVersion): DownloadTransferResult
}
