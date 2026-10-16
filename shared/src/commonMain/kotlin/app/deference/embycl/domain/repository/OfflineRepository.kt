package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyUserData
import app.deference.embycl.domain.model.download.*
import kotlinx.coroutines.flow.Flow

/** Explicit identity keeps background operations independent of mutable Session state. */
interface OfflineRepository {
    fun observeAccounts(): Flow<List<OfflineAccount>>
    fun observeDownloads(account: OfflineAccountKey): Flow<List<StoredDownload>>
    fun observeMedia(key: OfflineMediaKey): Flow<OfflineMedia?>
    /** Literal, case-insensitive substring match on title/series; completed records only. */
    fun searchDownloadedMedia(account: OfflineAccountKey, query: String): Flow<List<OfflineMedia>>
    fun observeAssets(key: DownloadKey): Flow<List<OfflineAsset>>
    fun observePlayback(key: OfflineMediaKey): Flow<OfflinePlayback?>
    fun observePendingSync(account: OfflineAccountKey): Flow<List<PendingPlaybackSync>>

    /** Idempotent by account/item/source; existing progress and source snapshots are preserved. */
    suspend fun enqueue(account: OfflineAccount, ready: DownloadResolution.Ready): StoredDownload
    suspend fun getDownload(key: DownloadKey): StoredDownload?

    /** Atomic optimistic mutation; null means absent/stale. The callback must be pure and fast. */
    suspend fun updateDownload(key: DownloadKey, expected: DownloadVersion, change: (StoredDownload) -> DownloadChange): StoredDownload?
    /** Refresh metadata without overwriting pending playback edits or their conflict baseline. */
    suspend fun refreshMedia(key: OfflineMediaKey, item: EmbyItem)
    suspend fun markMediaUnavailable(key: OfflineMediaKey, unavailable: Boolean)
    suspend fun saveAsset(key: DownloadKey, expected: DownloadVersion, asset: OfflineAsset): Boolean
    /** File layer must own the job and delete its files first. Keeps media/playback/pending sync. */
    suspend fun removeDownloadAfterFileDeletion(key: DownloadKey, expected: DownloadVersion): Boolean

    /** Save local progress and coalesce the latest sync intent in the same transaction. */
    suspend fun savePlayback(key: OfflineMediaKey, positionTicks: Long, played: Boolean): OfflinePlayback
    /** Reject a late player result if another session/edit has changed local progress. */
    suspend fun savePlaybackIfUnchanged(key: OfflineMediaKey, expectedRevision: Long, positionTicks: Long, played: Boolean): OfflinePlayback?
    /** Accept only a newer acknowledgement <= the local revision; retain/rebase any newer edit. */
    suspend fun acknowledgePlayback(key: OfflineMediaKey, revision: Long, serverState: EmbyUserData): Boolean

    /** Compare the complete observed intent, so user choices and late network callbacks cannot race. */
    suspend fun updateSyncJournal(expected: PendingPlaybackSync, journal: PlaybackSyncJournal): Boolean
    suspend fun resolvePlaybackSync(expected: PendingPlaybackSync, choice: PlaybackSyncChoice): Boolean
}
