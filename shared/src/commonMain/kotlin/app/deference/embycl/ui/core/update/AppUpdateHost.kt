package app.deference.embycl.ui.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.deference.embycl.domain.repository.AppRepo
import org.koin.compose.koinInject

@Composable
fun AppUpdateHost(appRepo: AppRepo = koinInject()) {
    val state by appRepo.updateState.collectAsState()
    LaunchedEffect(Unit) { appRepo.checkForUpdates(userInitiated = false) }
    if (state.dialogVisible) {
        UpdateAvailableDialog(
            appUpdate = state.update,
            onCheck = { appRepo.checkForUpdates() },
            onDownload = appRepo::downloadUpdate,
            onInstall = appRepo::installUpdate,
            onDismiss = appRepo::dismissUpdate,
        )
    }
}
