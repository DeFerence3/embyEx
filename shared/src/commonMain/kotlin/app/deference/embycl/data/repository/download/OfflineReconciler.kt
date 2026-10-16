package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.EmbyUserData
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import org.koin.core.annotation.Single

/** Serializes foreground reconciliation and explicit conflict choices. Payload files are never touched. */
@Single
class OfflineReconciler(private val repository: OfflineRepository, private val remote: OfflineSyncRemote,
    private val environment: DownloadEnvironment, private val session: OfflineUiSession,
    private val playback: OfflinePlaybackCoordinator, private val files: OfflineFileStore) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun <T> owned(block: suspend () -> T): T = mutex.withLock { files.withSyncLease(block) }
    suspend fun reconcile(connection: DownloadConnection) = owned {
        val account = OfflineAccountKey(connection.serverId, connection.userId)
        guard(connection)
        val pending = repository.observePendingSync(account).first().associateBy { it.playback.key }
        val downloads = repository.observeDownloads(account).first().associateBy { it.task.key.mediaKey() }
        for (key in pending.keys + downloads.keys) {
            guard(connection)
            if (playback.request.value?.key?.mediaKey() == key) continue
            val intent = current(key)
            if (intent?.journal?.block != null) continue
            if (repository.observeMedia(key).first()?.serverUnavailable == true) {
                intent?.let { repository.updateSyncJournal(it, PlaybackSyncJournal(block = PlaybackSyncBlock.ITEM_UNAVAILABLE)) }
                continue
            }
            try { reconcileItem(connection, key, intent) }
            catch (e: OfflineSyncException) {
                guard(connection)
                when (e.reason) {
                    OfflineSyncFailure.ITEM_UNAVAILABLE, OfflineSyncFailure.UNSUPPORTED -> {
                        if (e.reason == OfflineSyncFailure.ITEM_UNAVAILABLE) repository.markMediaUnavailable(key, true)
                        current(key)?.let { latest -> repository.updateSyncJournal(latest, latest.journal.copy(
                            block = if (e.reason == OfflineSyncFailure.ITEM_UNAVAILABLE) PlaybackSyncBlock.ITEM_UNAVAILABLE else PlaybackSyncBlock.UNSUPPORTED)) }
                    }
                    else -> throw e
                }
            }
        }
    }

    private suspend fun reconcileItem(connection: DownloadConnection, key: OfflineMediaKey, intent: PendingPlaybackSync?) {
        val remoteItem = remote.read(connection, key.itemId)
        guard(connection)
        if (playback.request.value?.key?.mediaKey() == key) return
        // Refresh before deciding, but keep the original pending baseline in the repository transaction.
        repository.refreshMedia(key, remoteItem.item)
        if (intent == null || current(key) != intent) return
        val state = remoteItem.item.userData ?: throw OfflineSyncException(OfflineSyncFailure.UNSUPPORTED)
        val local = intent.playback
        val attempt = intent.journal.attempt
        if (attempt != null) {
            val target = json.decodeFromJsonElement<EmbyUserData>(attempt.userData)
            if (state.sameWatchState(target)) {
                repository.acknowledgePlayback(key, attempt.revision, state)
                return
            }
            // Unknown delivery followed by another server edit must never be blindly replayed.
            if (local.serverBaseline == null || !state.sameWatchState(local.serverBaseline)) {
                conflict(intent, state); return
            }
        } else {
            if (state.playbackPositionTicks == local.positionTicks && state.played == local.played) {
                repository.acknowledgePlayback(key, local.revision, state); return
            }
            if (local.serverBaseline == null || !state.sameWatchState(local.serverBaseline)) {
                conflict(intent, state); return
            }
        }
        // Preserve the latest unrelated flags on a safe retry. Do not increment play counts or replay sessions.
        val target = JsonObject(remoteItem.userData.toMutableMap().apply {
            put("PlaybackPositionTicks", attempt?.userData?.get("PlaybackPositionTicks") ?: JsonPrimitive(local.positionTicks))
            put("Played", attempt?.userData?.get("Played") ?: JsonPrimitive(local.played))
        })
        val sent = PlaybackSyncAttempt(attempt?.revision ?: local.revision, target)
        if (!repository.updateSyncJournal(intent, PlaybackSyncJournal(attempt = sent))) return
        guard(connection)
        if (playback.request.value?.key?.mediaKey() == key) return
        remote.write(connection, key.itemId, target)
        guard(connection)
        val confirmed = remote.read(connection, key.itemId)
        guard(connection)
        val accepted = confirmed.item.userData ?: throw OfflineSyncException(OfflineSyncFailure.UNSUPPORTED)
        if (accepted.sameWatchState(json.decodeFromJsonElement<EmbyUserData>(target))) {
            repository.acknowledgePlayback(key, sent.revision, accepted)
        } else {
            current(key)?.let {
                if (local.serverBaseline.sameWatchState(accepted))
                    repository.updateSyncJournal(it, PlaybackSyncJournal(block = PlaybackSyncBlock.UNSUPPORTED))
                else conflict(it, accepted)
            }
        }
        if (playback.request.value?.key?.mediaKey() != key) repository.refreshMedia(key, confirmed.item)
    }

    private suspend fun conflict(intent: PendingPlaybackSync, state: EmbyUserData) {
        repository.updateSyncJournal(intent, PlaybackSyncJournal(PlaybackSyncBlock.CONFLICT, state))
    }
    suspend fun resolve(intent: PendingPlaybackSync, choice: PlaybackSyncChoice): Boolean = owned {
        if (session.account.value?.key != intent.playback.key.account || playback.request.value?.key?.mediaKey() == intent.playback.key) false
        else repository.resolvePlaybackSync(intent, choice)
    }
    suspend fun retryMetadata(key: OfflineMediaKey) = owned {
        if (session.account.value?.key == key.account) repository.markMediaUnavailable(key, false)
    }
    private suspend fun current(key: OfflineMediaKey) = repository.observePendingSync(key.account).first().firstOrNull { it.playback.key == key }
    private fun guard(connection: DownloadConnection) {
        if (environment.connection.value !== connection || session.account.value?.key != OfflineAccountKey(connection.serverId, connection.userId))
            throw CancellationException("Offline account changed")
    }
}
