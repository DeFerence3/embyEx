package app.deference.embycl.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.repository.DownloadsRepo
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.ui.screens.downloads.DownloadChoice
import kotlinx.coroutines.CancellationException
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmbyDetailsVM(
	@InjectedParam private val id: String,
	private val repository: EmbyRepository,
	private val downloads: DownloadsRepo,
) : ViewModel() {
	
	private val _state = MutableStateFlow(EmbyDetailsState())
	val state = _state.asStateFlow()
	private val _events = Channel<EmbyDetailsEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	
    private val playbackUpdates = Channel<EmbyDetailsAction.PlaybackUpdate>(capacity = Channel.CONFLATED)
    private var pendingSave: EmbyDetailsAction.PlaybackUpdate? = null
    private var reportedSession: String? = null

    init {
        viewModelScope.launch {
            for (update in playbackUpdates) savePlayback(update = update)
        }
		load()
	}
	
	fun onAction(action: EmbyDetailsAction) {
		when (action) {
			EmbyDetailsAction.Retry -> load()
			EmbyDetailsAction.Play -> preparePlayback()
			EmbyDetailsAction.PlayFromServer -> preparePlayback(fromServer = true)
			EmbyDetailsAction.Download -> requestDownload()
			is EmbyDetailsAction.SelectSource -> _state.update { s ->
				s.choice?.takeIf { c -> c.sources.any { it.key == action.key } }?.let { s.copy(choice = it.copy(selected = action.key)) } ?: s
			}
			EmbyDetailsAction.ConfirmDownload -> confirmDownload()
			EmbyDetailsAction.DismissDownload -> if (_state.value.choice?.submitting != true) _state.update { it.copy(choice = null) }
			EmbyDetailsAction.RemoveDownload -> removeDownload()
            is EmbyDetailsAction.PlaybackUpdate -> {
                if (_state.value.playback?.sessionId == action.request.sessionId) {
                    if (action.finished) _state.update { it.copy(playback = null, savingPlayback = true) }
                    playbackUpdates.trySend(element = action)
                }
            }
            EmbyDetailsAction.RetryPlaybackSave -> if (!_state.value.savingPlayback) {
                pendingSave?.let { update ->
                    _state.update { it.copy(savingPlayback = update.finished) }
                    pendingSave = null
                    playbackUpdates.trySend(element = update)
                }
            }
            EmbyDetailsAction.DismissPlaybackSaveError -> _state.update { it.copy(playbackSaveFailed = false) }

		}
	}

	private fun requestDownload() {
		val item = _state.value.content?.getOrNull() ?: return
		if (_state.value.checkingDownload) return
		_state.update { it.copy(checkingDownload = true, choice = null) }
		viewModelScope.launch {
			try {
				when (val request = downloads.download(item)) {
					is DownloadRequest.Choose -> _state.update { it.copy(choice = DownloadChoice(request.title, request.sources, request.selected)) }
					is DownloadRequest.Failed -> _events.send(EmbyDetailsEvent.Error(request.message))
				}
			} finally { _state.update { it.copy(checkingDownload = false) } }
		}
	}

	private fun confirmDownload() {
		val choice = _state.value.choice ?: return
		val key = choice.selected ?: return
		if (choice.submitting) return
		_state.update { it.copy(choice = choice.copy(submitting = true)) }
		viewModelScope.launch {
			try {
				val result = downloads.confirm(key)
				if (result.ok) _state.update { it.copy(choice = null) }
				result.message?.let { _events.send(EmbyDetailsEvent.Error(it)) }
			} finally { _state.update { it.copy(choice = it.choice?.copy(submitting = false)) } }
		}
	}

	private fun removeDownload() {
		viewModelScope.launch {
			val result = downloads.remove(id)
			result.message?.let { _events.send(EmbyDetailsEvent.Error(it)) }
			load()
		}
	}
	
	private fun preparePlayback(fromServer: Boolean = false) {
        val item = _state.value.content?.getOrNull() ?: return
        if (_state.value.preparingPlayback || _state.value.savingPlayback || _state.value.playbackSaveFailed || _state.value.playback != null) return
        _state.update { it.copy(preparingPlayback = true) }
        viewModelScope.launch {
            try {
                val request = if (fromServer) repository.playFromServer(item = item) else repository.play(item = item)
                if (request != null) _state.update { it.copy(playback = request) }
                else load()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _events.send(element = EmbyDetailsEvent.PlaybackFailed)
            } finally {
                _state.update { it.copy(preparingPlayback = false) }
            }
        }
    }

    private suspend fun savePlayback(
        update: EmbyDetailsAction.PlaybackUpdate,
    ) {
        val milliseconds = update.position.positionMs
        if (milliseconds == null || milliseconds !in 0..Long.MAX_VALUE / 10_000L) {
            if (update.finished) _state.update { it.copy(savingPlayback = false) }
            return
        }
        try {
            if (reportedSession != update.request.sessionId) {
                repository.reportPlayback(
                    request = update.request,
                    positionTicks = milliseconds * 10_000L,
                    event = EmbyPlaybackEvent.Started,
                    isPaused = update.position.paused,
                )
                reportedSession = update.request.sessionId
            }
            repository.reportPlayback(
                request = update.request,
                positionTicks = milliseconds * 10_000L,
                event = if (update.finished) EmbyPlaybackEvent.Stopped else EmbyPlaybackEvent.Progress,
                isPaused = update.position.paused,
            )
            pendingSave = null
            _state.update { it.copy(playbackSaveFailed = false) }
            if (update.finished) {
                _state.update { it.copy(savingPlayback = false) }
                load()
                val next = update.request.selectedIndex + 1
                if (update.position.completed && next < update.request.items.size && _state.value.playback == null) {
                    _state.update { it.copy(playback = update.request.copy(selectedIndex = next, sessionId = Uuid.random().toString())) }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            pendingSave = update
            _state.update { it.copy(playbackSaveFailed = true, savingPlayback = if (update.finished) false else it.savingPlayback) }
        }
    }

	private fun load() {
		_state.value = _state.value.copy(content = null)
		viewModelScope.launch {
			val result = try {
                Result.success(value = repository.item(id = id))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception = exception)
            }
			_state.value = _state.value.copy(content = result)
			result.exceptionOrNull()?.let { _events.send(EmbyDetailsEvent.Error(it.message ?: "Could not load item.")) }
		}
	}
}
