package app.deference.embcl.ui.screens.home

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
class HomeViewModel(
	private val repository: EmbyRepository,
) : ViewModel() {
	
	private val _state = MutableStateFlow(HomeState())
	val state = _state.asStateFlow()
	private val _events = Channel<HomeEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	
	init {
		load()
	}
	
	fun onAction(action: HomeAction) {
		when (action) {
			HomeAction.Retry -> load()
		}
	}
	
	private fun load() {
		_state.value = HomeState()
		viewModelScope.launch {
			val result = runCatching { repository.home() }
			_state.value = HomeState(result)
			result.exceptionOrNull()?.let { _events.send(HomeEvent.Error(it.message ?: "Could not load home.")) }
		}
	}
}
