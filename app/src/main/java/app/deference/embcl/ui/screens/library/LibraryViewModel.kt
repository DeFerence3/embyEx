package app.deference.embcl.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LibraryViewModel(
	@InjectedParam private val id: String,
	private val repository: EmbyRepository,
	sessionStore: EmbySessionStore,
) : ViewModel() {
	private val _state = MutableStateFlow(LibraryState(session = sessionStore.session.value))
	val state = _state.asStateFlow()
	private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()

	init { load() }

	fun onAction(action: LibraryAction) {
		when (action) { LibraryAction.Retry -> load() }
	}

	private fun load() {
		if (_state.value.session == null) return
		_state.value = _state.value.copy(content = null)
		viewModelScope.launch {
			val result = runCatching { repository.items(id) }
			_state.value = _state.value.copy(content = result)
			result.exceptionOrNull()?.let { _events.send(LibraryEvent.Error(it.message ?: "Could not load library.")) }
		}
	}
}
