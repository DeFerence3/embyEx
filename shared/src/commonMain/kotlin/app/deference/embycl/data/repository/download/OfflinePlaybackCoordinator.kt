package app.deference.embycl.data.repository.download

import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.channels.Channel
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

/** One local playback session; no network or remote catalogue dependency. */
@Single
class OfflinePlaybackCoordinator(
    private val repository: OfflineRepository,
    private val storage: OfflineStorage,
    private val session: OfflineUiSession,
) {
    private val ownership = Mutex()
    private data class Update(val result: LocalPlaybackResult, val finished: Boolean)
    private data class Pending(val id: String, val updates: Channel<Update>)
    private val pending = MutableStateFlow<Pending?>(null)
    private val _request = MutableStateFlow<LocalPlaybackRequest?>(null)
    val request = _request.asStateFlow()

    fun finish(sessionId: String, result: LocalPlaybackResult) {
        pending.value?.takeIf { it.id == sessionId }?.updates?.trySend(element = Update(result = result, finished = true))
    }

    fun checkpoint(sessionId: String, result: LocalPlaybackResult) {
        pending.value?.takeIf { it.id == sessionId }?.updates?.trySend(element = Update(result = result, finished = false))
    }

    suspend fun play(key: DownloadKey, downloadId: String): LocalPlaybackOutcome {
        if (!ownership.tryLock()) return LocalPlaybackOutcome.BUSY
        try {
            if (!session.browsingLocalFiles && session.account.value?.key != key.accountKey()) return LocalPlaybackOutcome.UNAVAILABLE
            return storage.withPlaybackFiles(key, downloadId) { row, media, subtitles ->
                val previous = repository.observePlayback(key.mediaKey()).first() ?: return@withPlaybackFiles LocalPlaybackOutcome.UNAVAILABLE
                if (!session.browsingLocalFiles && session.account.value?.key != key.accountKey()) return@withPlaybackFiles LocalPlaybackOutcome.UNAVAILABLE
                val id = Uuid.random().toString()
                val updates = Channel<Update>(capacity = Channel.UNLIMITED)
                pending.value = Pending(id = id, updates = updates)
                _request.value = LocalPlaybackRequest(id, key, row.id, row.media.item.name, media, subtitles, previous.positionTicks / 10_000)
                var revision = previous.revision
                var played = previous.played
                var conflict = false
                var saved = false
                try {
                    var outcome = LocalPlaybackOutcome.NO_PROGRESS
                    while (true) {
                        val update = updates.receive()
                        val result = update.result
                        val progress = localPlaybackProgress(result = result, previouslyPlayed = played)
                        if (progress != null && !conflict) {
                            val current = repository.savePlaybackIfUnchanged(
                                key = key.mediaKey(),
                                expectedRevision = revision,
                                positionTicks = progress.positionTicks,
                                played = progress.played,
                            )
                            if (current == null) conflict = true
                            else {
                                revision = current.revision
                                played = current.played
                                saved = true
                            }
                        }
                        if (update.finished) {
                            outcome = when {
                                result.error != null -> when (result.error) {
                                    LocalPlayerError.NOT_INSTALLED -> LocalPlaybackOutcome.PLAYER_MISSING
                                    LocalPlayerError.FAILED -> LocalPlaybackOutcome.PLAYER_FAILED
                                    LocalPlayerError.POSITION_UNSUPPORTED -> LocalPlaybackOutcome.POSITION_UNSUPPORTED
                                    LocalPlayerError.UNSUPPORTED_PLATFORM -> LocalPlaybackOutcome.UNSUPPORTED
                                }
                                conflict -> LocalPlaybackOutcome.CONFLICT
                                saved -> LocalPlaybackOutcome.SAVED
                                else -> LocalPlaybackOutcome.NO_PROGRESS
                            }
                            break
                        }
                    }
                    outcome
                } finally {
                    updates.close()
                }
            } ?: LocalPlaybackOutcome.UNAVAILABLE
        } catch (e: CancellationException) { throw e }
        catch (e: OfflineStorageException) { return if (e.reason == OfflineStorageFailure.BUSY) LocalPlaybackOutcome.BUSY else LocalPlaybackOutcome.STORAGE_FAILED }
        catch (_: Exception) { return LocalPlaybackOutcome.STORAGE_FAILED }
        finally { _request.value = null; pending.value = null; ownership.unlock() }
    }
}
