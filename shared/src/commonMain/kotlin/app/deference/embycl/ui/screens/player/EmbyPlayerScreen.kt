package app.deference.embycl.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EmbyPlayerScreen(
    val id: String,
    val fromServer: Boolean = false,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<EmbyPlayerViewModel>(parameters = { parametersOf(this) })
        val state by viewModel.state.collectAsStateWithLifecycle()
        val offline = koinInject<OfflinePlaybackCoordinator>()
        val localRequest by offline.request.collectAsStateWithLifecycle()
        LifecycleStartEffect(key1 = this) {
            viewModel.onAction(action = EmbyPlayerAction.Start)
            onStopOrDispose { }
        }
        LaunchedEffect(key1 = state.finished) {
            if (state.finished && navigator.backStack.lastOrNull() == this@EmbyPlayerScreen) navigator.goBack()
        }
        EmbyPlayerContent(
            state = state,
            localRequest = localRequest,
            onAction = viewModel::onAction,
            onLocalProgress = offline::checkpoint,
            onLocalResult = offline::finish,
            onBack = { navigator.goBack() },
        )
    }
	
}
