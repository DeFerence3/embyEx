package app.deference.embycl.ui.core.update

sealed interface AppUpdateAction {
    data class CheckForUpdates(val isUserInitiated: Boolean = true) : AppUpdateAction
    data object Download : AppUpdateAction
    data object DismissDialog : AppUpdateAction
    data object Install : AppUpdateAction
    data object AppResumed : AppUpdateAction
}
