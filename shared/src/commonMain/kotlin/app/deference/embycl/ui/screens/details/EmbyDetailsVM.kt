package app.deference.embycl.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.repository.DownloadsRepo
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.ui.screens.downloads.DownloadChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private var loading: Job? = null
	fun onAction(action: EmbyDetailsAction) {
		when (action) {
			EmbyDetailsAction.Retry -> load()
			EmbyDetailsAction.Download -> requestDownload()
			is EmbyDetailsAction.SelectSource -> _state.update { s ->
				s.choice?.takeIf { c -> c.sources.any { it.key == action.key } }?.let { s.copy(choice = it.copy(selected = action.key)) } ?: s
			}
			EmbyDetailsAction.ConfirmDownload -> confirmDownload()
			EmbyDetailsAction.DismissDownload -> if (_state.value.choice?.submitting != true) _state.update { it.copy(choice = null) }
			EmbyDetailsAction.RemoveDownload -> removeDownload()
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
	
	private fun load() {
        if (loading?.isActive == true) return
        if (_state.value.content?.isSuccess != true) _state.update { it.copy(content = null) }
		loading = viewModelScope.launch {
			val result = try {
                Result.success(value = repository.item(id = id))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception = exception)
            }
            _state.update { current ->
                if (result.isSuccess || current.content?.isSuccess != true) current.copy(content = result)
                else current
            }
			result.exceptionOrNull()?.let { _events.send(EmbyDetailsEvent.Error(it.message ?: "Could not load item.")) }
		}
	}
}
