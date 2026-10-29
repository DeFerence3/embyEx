package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import app.deference.embycl.domain.model.PlaybackPosition

@Composable
fun EmbeddedPlayerScreen(
    source: PlayerSource,
    onProgress: (PlaybackPosition) -> Unit,
    onClose: (PlaybackPosition) -> Unit,
) {
    PlayerWindow(
        title = source.title,
    ) { window ->
        EmbeddedPlayerContent(
            source = source,
            window = window,
            onProgress = onProgress,
            onClose = onClose,
        )
    }
}
