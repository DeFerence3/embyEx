package app.deference.embcl.ui.screens.libraries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LibrariesViewModel(private val repository: EmbyRepository) : ViewModel() {
	private val _state = MutableStateFlow(LibrariesState())
	val state = _state.asStateFlow()
	private val _events = Channel<LibrariesEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()

	init { load() }

	fun onAction(action: LibrariesAction) {
		when (action) { LibrariesAction.Retry -> load() }
	}

	private fun load() {
		_state.value = LibrariesState()
		viewModelScope.launch {
			val result = runCatching { repository.libraries() }
			_state.value = LibrariesState(result)
			result.exceptionOrNull()?.let { _events.send(LibrariesEvent.Error(it.message ?: "Could not load libraries.")) }
		}
	}
}
