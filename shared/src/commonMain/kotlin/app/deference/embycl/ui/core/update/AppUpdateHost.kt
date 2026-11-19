package app.deference.embycl.ui.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.BuildConfig
import org.koin.compose.koinInject

@Composable
fun AppUpdateHost() {
    val vm = koinInject<AppUpdateVM>()
    val onAction = vm::onAction
    val state by vm.state.collectAsStateWithLifecycle()
    //if you are running app directly from source code (debug build) disable startup update check
    if (BuildConfig.VERSION_NAME != "1.0.0") LaunchedEffect(Unit) { onAction(AppUpdateAction.CheckForUpdates(isUserInitiated = false)) }
    // Returning from the system installer or its permission screen resumes the app.
    LifecycleResumeEffect(key1 = Unit) {
        onAction(AppUpdateAction.AppResumed)
        onPauseOrDispose { }
    }
    if (state.dialogVisible) {
        UpdateAvailableDialog(
            appUpdate = state.update,
            onCheck = { onAction(AppUpdateAction.CheckForUpdates()) },
            onDownload = { onAction(AppUpdateAction.Download) },
            onInstall = { onAction(AppUpdateAction.Install) },
            onDismiss = { onAction(AppUpdateAction.DismissDialog) },
        )
    }
}
