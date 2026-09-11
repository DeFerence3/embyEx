package app.deference.embcl.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SearchViewModel(private val repository: EmbyRepository) : ViewModel() {
	private val _state = MutableStateFlow(SearchState())
	val state = _state.asStateFlow()
	private val _events = Channel<SearchEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	private var searchJob: Job? = null

	fun onAction(action: SearchAction) {
		when (action) {
			is SearchAction.QueryChanged -> {
				_state.value = _state.value.copy(query = action.query)
				search(debounce = true)
			}
			SearchAction.Retry -> search(debounce = false)
		}
	}

	private fun search(debounce: Boolean) {
		searchJob?.cancel()
		val query = _state.value.query.trim()
		if (query.isBlank()) {
			_state.value = SearchState()
			return
		}
		searchJob = viewModelScope.launch {
			if (debounce) delay(350)
			_state.value = _state.value.copy(isLoading = true, error = null)
			runCatching { repository.search(query) }
				.onSuccess { _state.value = _state.value.copy(results = it, isLoading = false) }
				.onFailure {
					val message = it.message ?: "Search failed."
					_state.value = _state.value.copy(isLoading = false, error = message)
					_events.send(SearchEvent.Error(message))
				}
		}
	}
}
