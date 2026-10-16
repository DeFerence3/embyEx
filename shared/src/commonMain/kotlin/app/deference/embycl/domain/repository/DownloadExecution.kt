package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.download.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Availability is a transport hint, never proof that the selected Emby server responds. */
data class DownloadNetwork(val available: Boolean, val unmetered: Boolean? = null)

interface DownloadEnvironment {
    val connection: StateFlow<DownloadConnection?>
    val network: StateFlow<DownloadNetwork>
    val unmeteredOnly: Flow<Boolean>
    suspend fun setUnmeteredOnly(value: Boolean)
}

enum class DownloadScheduling { SCHEDULED, REQUIRES_USER_ACTION, UNAVAILABLE }

interface DownloadScheduler {
    /** Call from the visible UI's download/resume action. Never infer user initiation at boot. */
    suspend fun requestExecution(userInitiated: Boolean): DownloadScheduling
}

interface DownloadAssetFetcher {
    suspend fun hasPending(download: StoredDownload): Boolean
    /** Optional failures must not invalidate completed original media. */
    suspend fun fetch(connection: DownloadConnection, download: StoredDownload)
}

data class DownloadQueueActivity(val download: StoredDownload)
data class DownloadQueueResult(val retryAtEpochMillis: Long? = null, val storageFailure: OfflineStorageFailure? = null)
