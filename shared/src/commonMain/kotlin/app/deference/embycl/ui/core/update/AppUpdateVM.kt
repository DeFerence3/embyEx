package app.deference.embycl.ui.core.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.domain.repository.AppRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class AppUpdateVM(
    private val appRepo: AppRepo,
) : ViewModel() {
    private val _state = MutableStateFlow(value = AppUpdateState())
    val state = _state.asStateFlow()

    private var operation: Job? = null
    private var checkedAutomatically = false

    fun onAction(action: AppUpdateAction) {
        when (action) {
            is AppUpdateAction.CheckForUpdates -> checkForUpdates(userInitiated = action.isUserInitiated)
            AppUpdateAction.Download -> launchOperation {
                appRepo.downloadUpdate().collect { update -> publish(update = update) }
            }
            AppUpdateAction.Install -> launchOperation {
                publish(update = appRepo.installUpdate())
            }
            AppUpdateAction.AppResumed -> if (operation?.isActive != true) {
                publish(update = appRepo.onAppResumed(current = _state.value.update))
            }
            AppUpdateAction.DismissDialog -> _state.update { it.copy(dialogVisible = false) }
        }
    }

    private fun checkForUpdates(userInitiated: Boolean) {
        if (userInitiated) {
            _state.update { it.copy(dialogVisible = true) }
        } else {
            // The host re-enters composition, for example when an Android activity is recreated.
            if (checkedAutomatically) return
            checkedAutomatically = true
        }
        // A manual check joins a running operation and keeps a download instead of replacing it.
        if (_state.value.update.hasDownload()) return
        launchOperation {
            publish(update = AppUpdate.Checking)
            val update = appRepo.checkForUpdates(userInitiated = userInitiated).getOrElse { failure ->
                AppUpdate.Failed(
                    message = failure.message ?: "Could not check for updates. Check your connection and try again.",
                    stage = UpdateStage.Check,
                )
            }
            // An automatic check only opens the dialog when there is something to install.
            val reveal = !userInitiated && (update is AppUpdate.Available || update is AppUpdate.ReadyToInstall)
            publish(
                update = update,
                reveal = reveal,
            )
        }
    }

    private fun launchOperation(block: suspend () -> Unit) {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch { block() }
    }

    private fun publish(
        update: AppUpdate,
        reveal: Boolean = false,
    ) = _state.update { it.copy(update = update, dialogVisible = it.dialogVisible || reveal) }

    private fun AppUpdate.hasDownload(): Boolean = when (this) {
        is AppUpdate.Downloading,
        is AppUpdate.ReadyToInstall,
        is AppUpdate.AwaitingPermission,
        is AppUpdate.InstallerLaunched, -> true
        AppUpdate.Idle,
        AppUpdate.Checking,
        AppUpdate.NotAvailable,
        is AppUpdate.Available,
        is AppUpdate.Failed, -> false
    }
}
