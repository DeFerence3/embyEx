package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import org.koin.compose.koinInject

/** App-level host keeps the file lease and playback alive across catalogue screen changes. */
@Composable
internal fun OfflinePlaybackHost() {
    val coordinator = koinInject<OfflinePlaybackCoordinator>()
    val request by coordinator.request.collectAsStateWithLifecycle()
    LocalPlaybackEffect(
        request = request,
        onResult = coordinator::finish,
        onProgress = coordinator::checkpoint,
    )
}
