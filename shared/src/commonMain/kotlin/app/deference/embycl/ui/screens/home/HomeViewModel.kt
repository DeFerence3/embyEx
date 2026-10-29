package app.deference.embycl.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.repository.EmbyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
	private val repository: EmbyRepository,
) : ViewModel() {
	
	private val _state = MutableStateFlow(HomeState())
	val state = _state.asStateFlow()
	private val _events = Channel<HomeEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch { repository.changes.collect { load() } }
    }
	
	fun onAction(action: HomeAction) {
		when (action) {
			HomeAction.Retry,
			HomeAction.Refresh -> load()
		}
	}
	
	private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = try {
                Result.success(value = repository.home())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception = exception)
            }
			_state.value = HomeState(result)
			result.exceptionOrNull()?.let { _events.send(HomeEvent.Error(it.message ?: "Could not load home.")) }
		}
	}
}
