package app.deference.embcl.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(private val repository: EmbyRepository) : ViewModel() {
	private val _state = MutableStateFlow(SettingsState())
	val state = _state.asStateFlow()

	init {
		refreshServerInfo()
	}

	fun refreshServerInfo() {
		if (_state.value.isLoading) return
		_state.update { it.copy(isLoading = true, error = null) }
		viewModelScope.launch {
			try {
				val details = repository.serverInfo()
				_state.update { it.copy(isLoading = false, serverDetails = details) }
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				_state.update {
					it.copy(isLoading = false, error = "Could not refresh server information. Check your connection and try again.")
				}
			}
		}
	}
}
