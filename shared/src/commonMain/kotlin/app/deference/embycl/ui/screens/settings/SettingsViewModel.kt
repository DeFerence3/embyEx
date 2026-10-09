package app.deference.embycl.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.repository.AppRepo
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.data.repository.ReleaseNotesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SettingsViewModel(
	private val repository: EmbyRepository,
	private val appRepo: AppRepo,
	private val releaseNotesRepository: ReleaseNotesRepository,
) : ViewModel() {
	private val _state = MutableStateFlow(SettingsState())
	val state = _state.asStateFlow()

	val updateState = appRepo.updateState

	init { refreshServerInfo() }

	fun checkForUpdates() = appRepo.checkForUpdates()

	fun loadCurrentBuildChangelog() {
		if (_state.value.isChangelogLoading || _state.value.changelog?.isSuccess == true) return
		_state.update { it.copy(isChangelogLoading = true, changelog = null) }
		viewModelScope.launch {
			try {
				val notes = releaseNotesRepository.currentBuildChangelog()
				_state.update { it.copy(isChangelogLoading = false, changelog = Result.success(notes)) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				val message = if (e is IllegalStateException) e.message else null
				_state.update { it.copy(isChangelogLoading = false, changelog = Result.failure(
					IllegalStateException(message ?: "Could not load release notes. Check your connection and try again.")
				)) }
			}
		}
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
