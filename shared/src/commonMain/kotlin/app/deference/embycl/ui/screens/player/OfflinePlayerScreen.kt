package app.deference.embycl.ui.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.screens.downloads.LocalPlaybackEffect
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

/** A local session owns a checked file lease; an expired session returns to Downloads. */
@Serializable
internal data class OfflinePlayerScreen(
    val sessionId: String,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val coordinator = koinInject<OfflinePlaybackCoordinator>()
        val request by coordinator.request.collectAsStateWithLifecycle()
        val active = request?.takeIf { it.sessionId == sessionId }
        LaunchedEffect(key1 = active == null) {
            if (active == null && navigator.backStack.lastOrNull() == this@OfflinePlayerScreen) navigator.goBack()
        }
        LocalPlaybackEffect(
            request = active,
            onResult = coordinator::finish,
            onProgress = coordinator::checkpoint,
        )
    }
}
