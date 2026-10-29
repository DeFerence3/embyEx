package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import org.koin.compose.koinInject
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.screens.player.EmbyPlayerScreen
import app.deference.embycl.ui.screens.player.OfflinePlayerScreen

/** Downloads can start playback from several destinations; navigate once per file lease. */
@Composable
internal fun OfflinePlaybackHost(
    navigator: Navigator,
) {
    val coordinator = koinInject<OfflinePlaybackCoordinator>()
    val request by coordinator.request.collectAsStateWithLifecycle()
    LaunchedEffect(key1 = request?.sessionId) {
        request?.let { active ->
            if (navigator.backStack.lastOrNull() !is EmbyPlayerScreen &&
                navigator.backStack.lastOrNull() != OfflinePlayerScreen(sessionId = active.sessionId)
            ) navigator.goTo(destination = OfflinePlayerScreen(sessionId = active.sessionId))
        }
    }
}
