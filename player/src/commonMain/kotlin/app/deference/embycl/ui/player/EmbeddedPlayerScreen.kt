package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import app.deference.embycl.domain.model.PlaybackPosition

@Composable
fun EmbeddedPlayerScreen(
    source: PlayerSource,
    onProgress: (PlaybackPosition) -> Unit,
    onClose: (PlaybackPosition) -> Unit,
    /**
     * Called instead of [onClose] when the user moves to another queue entry (previous, next, Up next, or
     * the next episode starting by itself). Receives the final position of this episode and the target id.
     */
    onNavigate: ((PlaybackPosition, String) -> Unit)? = null,
) {
    PlayerWindow(
        title = source.title,
    ) { window ->
        EmbeddedPlayerContent(
            source = source,
            window = window,
            onProgress = onProgress,
            onClose = onClose,
            onNavigate = onNavigate,
        )
    }
}
