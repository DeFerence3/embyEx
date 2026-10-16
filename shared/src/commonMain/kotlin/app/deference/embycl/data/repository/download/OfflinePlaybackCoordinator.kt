package app.deference.embycl.data.repository.download

import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
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
    private data class Pending(val id: String, val result: CompletableDeferred<LocalPlaybackResult>)
    private val pending = MutableStateFlow<Pending?>(null)
    private val _request = MutableStateFlow<LocalPlaybackRequest?>(null)
    val request = _request.asStateFlow()

    fun finish(sessionId: String, result: LocalPlaybackResult) {
        pending.value?.takeIf { it.id == sessionId }?.result?.complete(result)
    }

    suspend fun play(key: DownloadKey, downloadId: String): LocalPlaybackOutcome {
        if (!ownership.tryLock()) return LocalPlaybackOutcome.BUSY
        try {
            if (!session.browsingLocalFiles && session.account.value?.key != key.accountKey()) return LocalPlaybackOutcome.UNAVAILABLE
            return storage.withPlaybackFiles(key, downloadId) { row, media, subtitles ->
                val previous = repository.observePlayback(key.mediaKey()).first() ?: return@withPlaybackFiles LocalPlaybackOutcome.UNAVAILABLE
                if (!session.browsingLocalFiles && session.account.value?.key != key.accountKey()) return@withPlaybackFiles LocalPlaybackOutcome.UNAVAILABLE
                val id = Uuid.random().toString()
                val response = CompletableDeferred<LocalPlaybackResult>()
                pending.value = Pending(id, response)
                _request.value = LocalPlaybackRequest(id, key, row.id, row.media.item.name, media, subtitles, previous.positionTicks / 10_000)
                val result = response.await()
                val progress = localPlaybackProgress(result, previous.played)
                when {
                    result.error != null -> when (result.error) {
                        LocalPlayerError.NOT_INSTALLED -> LocalPlaybackOutcome.PLAYER_MISSING
                        LocalPlayerError.FAILED -> LocalPlaybackOutcome.PLAYER_FAILED
                        LocalPlayerError.POSITION_UNSUPPORTED -> LocalPlaybackOutcome.POSITION_UNSUPPORTED
                        LocalPlayerError.UNSUPPORTED_PLATFORM -> LocalPlaybackOutcome.UNSUPPORTED
                    }
                    progress == null -> LocalPlaybackOutcome.NO_PROGRESS
                    repository.savePlaybackIfUnchanged(key.mediaKey(), previous.revision, progress.positionTicks, progress.played) == null -> LocalPlaybackOutcome.CONFLICT
                    else -> LocalPlaybackOutcome.SAVED
                }
            } ?: LocalPlaybackOutcome.UNAVAILABLE
        } catch (e: CancellationException) { throw e }
        catch (e: OfflineStorageException) { return if (e.reason == OfflineStorageFailure.BUSY) LocalPlaybackOutcome.BUSY else LocalPlaybackOutcome.STORAGE_FAILED }
        catch (_: Exception) { return LocalPlaybackOutcome.STORAGE_FAILED }
        finally { _request.value = null; pending.value = null; ownership.unlock() }
    }
}
