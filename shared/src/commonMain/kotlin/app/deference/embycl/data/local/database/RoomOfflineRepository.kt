package app.deference.embycl.data.local.database

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import app.deference.embycl.data.local.database.dao.DownloadWithMedia
import app.deference.embycl.data.local.database.entity.AccountEntity
import app.deference.embycl.data.local.database.entity.AssetEntity
import app.deference.embycl.data.local.database.entity.DownloadEntity
import app.deference.embycl.data.local.database.entity.MediaEntity
import app.deference.embycl.data.local.database.entity.OutboxEntity
import app.deference.embycl.data.local.database.entity.PlaybackEntity
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyUserData
import app.deference.embycl.domain.model.download.DownloadChange
import app.deference.embycl.domain.model.download.DownloadKey
import app.deference.embycl.domain.model.download.DownloadProbe
import app.deference.embycl.domain.model.download.DownloadResolution
import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.domain.model.download.DownloadTask
import app.deference.embycl.domain.model.download.DownloadVersion
import app.deference.embycl.domain.model.download.LocalMediaState
import app.deference.embycl.domain.model.download.OfflineAccount
import app.deference.embycl.domain.model.download.OfflineAccountKey
import app.deference.embycl.domain.model.download.OfflineAsset
import app.deference.embycl.domain.model.download.OfflineAssetKind
import app.deference.embycl.domain.model.download.OfflineAssetState
import app.deference.embycl.domain.model.download.OfflineMedia
import app.deference.embycl.domain.model.download.OfflineMediaKey
import app.deference.embycl.domain.model.download.OfflinePlayback
import app.deference.embycl.domain.model.download.PendingPlaybackSync
import app.deference.embycl.domain.model.download.PlaybackSyncBlock
import app.deference.embycl.domain.model.download.PlaybackSyncChoice
import app.deference.embycl.domain.model.download.PlaybackSyncJournal
import app.deference.embycl.domain.model.download.StoredDownload
import app.deference.embycl.domain.model.download.accountKey
import app.deference.embycl.domain.model.download.mediaKey
import app.deference.embycl.domain.repository.OfflineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single(binds = [OfflineRepository::class])
class RoomOfflineRepository(factory: OfflineDatabaseFactory) : OfflineRepository {
    private val database by lazy(factory::open)
    private val accountDao get() = database.accountDao
    private val mediaDao get() = database.mediaDao
    private val downloadDao get() = database.downloadDao
    private val assetDao get() = database.assetDao
    private val playbackDao get() = database.playbackDao
    private val outboxDao get() = database.outboxDao
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // Cold flows defer database/directory initialization off the caller's UI thread.
    private fun <D, T> observe(dao: () -> D, query: (D) -> Flow<T>): Flow<T> =
        flow { emitAll(query(dao())) }.flowOn(Dispatchers.IO)

    private suspend fun <T> write(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        database.useWriterConnection { it.immediateTransaction { block() } }
    }

    override fun observeAccounts() = observe({ accountDao }) { it.observeAccounts() }.map { rows ->
        rows.map { OfflineAccount(OfflineAccountKey(it.serverId, it.userId), it.serverName, it.userName) }
    }
    override fun observeDownloads(account: OfflineAccountKey) = observe({ downloadDao }) { it.observeDownloads(account.serverId, account.userId) }
        .map { rows -> rows.map { it.model() } }
    override fun observeMedia(key: OfflineMediaKey) = observe({ mediaDao }) { it.observeMedia(key.account.serverId, key.account.userId, key.itemId) }
        .map { it?.model() }
    override fun searchDownloadedMedia(account: OfflineAccountKey, query: String) = observe({ mediaDao }) {
        it.search(account.serverId, account.userId, query.trim().lowercase())
    }.map { rows -> rows.map { it.model() } }
    override fun observeAssets(key: DownloadKey) = observe({ assetDao }) { it.observeAssets(key.serverId, key.userId, key.itemId, key.mediaSourceId) }
        .map { rows -> rows.map { OfflineAsset(it.assetId, OfflineAssetKind.valueOf(it.kind), OfflineAssetState.valueOf(it.state), it.relativePath, it.sizeBytes, it.streamIndex) } }
    override fun observePlayback(key: OfflineMediaKey) = observe({ playbackDao }) { it.observePlayback(key.account.serverId, key.account.userId, key.itemId) }
        .map { it?.model() }
    override fun observePendingSync(account: OfflineAccountKey) = observe({ outboxDao }) { it.observeOutbox(account.serverId, account.userId) }
        .map { rows -> rows.map { it.model() } }

    override suspend fun getDownload(key: DownloadKey): StoredDownload? = withContext(Dispatchers.IO) { find(key)?.model() }
    private suspend fun find(key: DownloadKey) = downloadDao.download(key.serverId, key.userId, key.itemId, key.mediaSourceId)

    override suspend fun enqueue(account: OfflineAccount, ready: DownloadResolution.Ready): StoredDownload = write {
        val key = ready.source.key
        require(account.key == key.accountKey()) { "Download account does not match." }
        validateItem(key.mediaKey(), ready.item)
        validateProbe(ready.probe)
        require(ready.source.container.isNotBlank())
        require(ready.source.sizeBytes == null || ready.source.sizeBytes > 0)
        require(ready.source.sizeBytes == null || ready.probe.totalBytes == null || ready.source.sizeBytes == ready.probe.totalBytes)
        find(key)?.let { return@write it.model() }
        val now = Clock.System.now().toEpochMilliseconds()
        accountDao.saveAccount(AccountEntity(key.serverId, key.userId, account.serverName, account.userName, now))
        storeMedia(key.mediaKey(), ready.item, now)
        val task = DownloadTask.queued(key, ready.probe.totalBytes ?: ready.source.sizeBytes)
        downloadDao.saveDownload(
	        DownloadEntity(
				Uuid.random().toString(), key.serverId, key.userId, key.itemId, key.mediaSourceId,
				task.status.name, json.encodeToString(task), json.encodeToString(ready.source), json.encodeToString(ready.probe),
				null, LocalMediaState.NOT_READY.name, now, now, 0,
			)
        )
        checkNotNull(find(key)).model()
    }

    override suspend fun updateDownload(key: DownloadKey, expected: DownloadVersion, change: (StoredDownload) -> DownloadChange): StoredDownload? = write {
        val row = find(key) ?: return@write null
        if (row.download.revision != expected.revision || row.download.id != expected.downloadId) return@write null
        val updated = change(row.model())
        require(updated.task.key == key) { "A download's identity cannot change." }
        require(updated.source.key == key && updated.source.container.isNotBlank())
        require(updated.source.sizeBytes == null || updated.source.sizeBytes > 0)
        if (row.download.status == DownloadStatus.COMPLETED.name) {
            require(updated.source == row.model().source) { "A completed source cannot be replaced." }
        }
        validateProbe(updated.probe)
        require(updated.probe.totalBytes == null || updated.task.progress.totalBytes == null || updated.probe.totalBytes == updated.task.progress.totalBytes)
        updated.relativeMediaPath?.let { validatePath(row.download.id, it) }
        if (updated.task.status == DownloadStatus.COMPLETED) {
            require(updated.relativeMediaPath != null && updated.localMediaState != LocalMediaState.NOT_READY)
        } else require(updated.localMediaState == LocalMediaState.NOT_READY)
        downloadDao.saveDownload(row.download.copy(
            taskJson = json.encodeToString(updated.task), status = updated.task.status.name,
            sourceJson = json.encodeToString(updated.source),
            probeJson = json.encodeToString(updated.probe), relativeMediaPath = updated.relativeMediaPath,
            localMediaState = updated.localMediaState.name, updatedAt = Clock.System.now().toEpochMilliseconds(),
            revision = nextRevision(expected.revision),
        ))
        checkNotNull(find(key)).model()
    }

    override suspend fun refreshMedia(key: OfflineMediaKey, item: EmbyItem) = write {
        checkNotNull(mediaDao.media(key.account.serverId, key.account.userId, key.itemId)) { "Media is not in the offline catalog." }
        storeMedia(key, item, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun markMediaUnavailable(key: OfflineMediaKey, unavailable: Boolean) = write {
        mediaDao.markMediaUnavailable(key.account.serverId, key.account.userId, key.itemId, unavailable)
    }

    private suspend fun storeMedia(key: OfflineMediaKey, item: EmbyItem, now: Long) {
        validateItem(key, item)
        item.userData?.let(::validateUserData)
        mediaDao.saveMedia(
	        MediaEntity(
		        key.account.serverId, key.account.userId, key.itemId, item.name,
				listOfNotNull(item.name, item.seriesName).joinToString("\n").lowercase(), json.encodeToString(item), now
	        )
        )
        val old = playbackDao.playback(key.account.serverId, key.account.userId, key.itemId)
        val pending = outboxDao.outbox(key.account.serverId, key.account.userId, key.itemId)
        if (old == null) {
            playbackDao.savePlayback(
	            PlaybackEntity(
	            key.account.serverId, key.account.userId, key.itemId,
				item.userData?.playbackPositionTicks ?: 0, item.userData?.played ?: false, 0, 0,
				item.userData?.let { json.encodeToString(it) }, now
            )
            )
        } else if (pending == null && item.userData != null) {
            val revision = if (old.positionTicks != item.userData.playbackPositionTicks || old.played != item.userData.played)
                nextRevision(old.revision) else old.revision
            playbackDao.savePlayback(old.copy(positionTicks = item.userData.playbackPositionTicks, played = item.userData.played,
                revision = revision, serverBaselineJson = json.encodeToString(item.userData), acknowledgedRevision = revision, updatedAt = now))
        }
    }

    override suspend fun saveAsset(key: DownloadKey, expected: DownloadVersion, asset: OfflineAsset): Boolean = write {
        val row = find(key)?.download ?: return@write false
        if (row.revision != expected.revision || row.id != expected.downloadId) return@write false
        asset.relativePath?.let { validatePath(row.id, it) }
        assetDao.saveAsset(
	        AssetEntity(
		        row.id, asset.id, asset.kind.name, asset.state.name, asset.relativePath,
				asset.sizeBytes, asset.streamIndex, Clock.System.now().toEpochMilliseconds()
	        )
        )
        true
    }

    override suspend fun removeDownloadAfterFileDeletion(key: DownloadKey, expected: DownloadVersion): Boolean = write {
        downloadDao.deleteDownload(key.serverId, key.userId, key.itemId, key.mediaSourceId, expected.downloadId, expected.revision) == 1
    }

    override suspend fun savePlayback(key: OfflineMediaKey, positionTicks: Long, played: Boolean): OfflinePlayback =
        checkNotNull(saveProgress(key, null, positionTicks, played))

    override suspend fun savePlaybackIfUnchanged(key: OfflineMediaKey, expectedRevision: Long, positionTicks: Long, played: Boolean): OfflinePlayback? =
        saveProgress(key, expectedRevision, positionTicks, played)

    private suspend fun saveProgress(key: OfflineMediaKey, expectedRevision: Long?, positionTicks: Long, played: Boolean): OfflinePlayback? = write {
        require(positionTicks >= 0)
        val old = checkNotNull(playbackDao.playback(key.account.serverId, key.account.userId, key.itemId)) { "Media is not in the offline catalog." }
        if (expectedRevision != null && old.revision != expectedRevision) return@write null
        val pending = outboxDao.outbox(key.account.serverId, key.account.userId, key.itemId)
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = old.copy(positionTicks = positionTicks, played = played, revision = nextRevision(old.revision), updatedAt = now)
        playbackDao.savePlayback(updated)
        outboxDao.saveOutbox(
	        OutboxEntity(
		        old.serverId, old.userId, old.itemId, positionTicks, played, updated.revision,
				updated.acknowledgedRevision, updated.serverBaselineJson, pending?.createdAt ?: now, now, pending?.syncJson
	        )
        )
        updated.model()
    }

    override suspend fun acknowledgePlayback(key: OfflineMediaKey, revision: Long, serverState: EmbyUserData): Boolean = write {
        validateUserData(serverState)
        val old = playbackDao.playback(key.account.serverId, key.account.userId, key.itemId) ?: return@write false
        val pending = outboxDao.outbox(key.account.serverId, key.account.userId, key.itemId) ?: return@write false
        if (revision <= old.acknowledgedRevision || revision > old.revision) return@write false
        val baseline = json.encodeToString(serverState)
        playbackDao.savePlayback(old.copy(acknowledgedRevision = revision, serverBaselineJson = baseline))
        if (pending.revision == revision) {
            check(outboxDao.deleteAcknowledged(old.serverId, old.userId, old.itemId, revision) == 1)
        } else {
            // A request finished after another local seek/play. Preserve that newer intent,
            // but compare its next server read against the state our earlier request wrote.
            outboxDao.saveOutbox(pending.copy(acknowledgedRevision = revision, serverBaselineJson = baseline, syncJson = null))
        }
        true
    }

    override suspend fun updateSyncJournal(expected: PendingPlaybackSync, journal: PlaybackSyncJournal): Boolean = write {
        val key = expected.playback.key
        val row = outboxDao.outbox(key.account.serverId, key.account.userId, key.itemId) ?: return@write false
        if (row.model() != expected) return@write false
        outboxDao.saveOutbox(row.copy(syncJson = json.encodeToString(journal)))
        true
    }

    override suspend fun resolvePlaybackSync(expected: PendingPlaybackSync, choice: PlaybackSyncChoice): Boolean = write {
        val key = expected.playback.key
        val row = outboxDao.outbox(key.account.serverId, key.account.userId, key.itemId) ?: return@write false
        if (row.model() != expected || expected.journal.block == null) return@write false
        val old = checkNotNull(playbackDao.playback(key.account.serverId, key.account.userId, key.itemId))
        val remote = expected.journal.serverState
        if (choice != PlaybackSyncChoice.RETRY && (expected.journal.block != PlaybackSyncBlock.CONFLICT || remote == null)) return@write false
        val baseline = remote?.let { json.encodeToString(it) } ?: row.serverBaselineJson
        if (choice == PlaybackSyncChoice.USE_SERVER) {
            val revision = nextRevision(old.revision)
            playbackDao.savePlayback(old.copy(positionTicks = remote!!.playbackPositionTicks, played = remote.played,
                revision = revision, acknowledgedRevision = revision, serverBaselineJson = baseline))
            check(outboxDao.deleteAcknowledged(row.serverId, row.userId, row.itemId, row.revision) == 1)
        } else {
            // KEEP_LOCAL accepts only the displayed remote baseline; another server edit conflicts again.
            if (choice == PlaybackSyncChoice.RETRY && expected.journal.block == PlaybackSyncBlock.CONFLICT) return@write false
            playbackDao.savePlayback(old.copy(serverBaselineJson = baseline))
            outboxDao.saveOutbox(row.copy(serverBaselineJson = baseline, syncJson = null))
            if (choice == PlaybackSyncChoice.RETRY) mediaDao.markMediaUnavailable(key.account.serverId, key.account.userId, key.itemId, false)
        }
        true
    }

    private fun OutboxEntity.model() = PendingPlaybackSync(OfflinePlayback(
        OfflineMediaKey(OfflineAccountKey(serverId, userId), itemId), positionTicks, played, revision,
        acknowledgedRevision, serverBaselineJson?.let { json.decodeFromString<EmbyUserData>(it) }, updatedAt,
    ), createdAt, syncJson?.let { json.decodeFromString<PlaybackSyncJournal>(it) } ?: PlaybackSyncJournal())

    private fun DownloadWithMedia.model() = StoredDownload(
        download.id, json.decodeFromString(download.taskJson), json.decodeFromString(download.sourceJson),
        json.decodeFromString(download.probeJson), OfflineMedia(json.decodeFromString(snapshotJson), serverObservedAt, serverUnavailable),
        download.relativeMediaPath, LocalMediaState.valueOf(download.localMediaState), download.createdAt, download.updatedAt, download.revision,
    )
    private fun MediaEntity.model() = OfflineMedia(json.decodeFromString<EmbyItem>(snapshotJson), serverObservedAt, serverUnavailable)
    private fun PlaybackEntity.model() = OfflinePlayback(OfflineMediaKey(OfflineAccountKey(serverId, userId), itemId),
        positionTicks, played, revision, acknowledgedRevision, serverBaselineJson?.let { json.decodeFromString<EmbyUserData>(it) }, updatedAt)

    private fun validateItem(key: OfflineMediaKey, item: EmbyItem) {
        require(item.id == key.itemId && (item.serverId == null || item.serverId == key.account.serverId)) { "Media identity does not match." }
    }
    private fun validateProbe(probe: DownloadProbe) { require(probe.totalBytes == null || probe.totalBytes > 0) }
    private fun validateUserData(data: EmbyUserData) {
        require(data.playbackPositionTicks >= 0 && (data.playCount == null || data.playCount >= 0))
    }
    private fun nextRevision(revision: Long): Long {
        check(revision < Long.MAX_VALUE) { "Revision exhausted." }
        return revision + 1
    }
    private fun validatePath(downloadId: String, path: String) {
        val segments = path.split('/')
        require(segments.size >= 2 && segments.first() == downloadId && segments.all {
            it.matches(Regex("[a-zA-Z0-9._-]+")) && it != "." && it != ".." && !it.endsWith('.')
        }) { "Path must be relative and confined to this download's directory." }
    }
}
