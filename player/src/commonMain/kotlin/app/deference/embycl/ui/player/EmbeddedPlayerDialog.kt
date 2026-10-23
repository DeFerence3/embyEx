package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.deference.embycl.domain.model.PlaybackPosition
import kotlinx.coroutines.launch

@Composable
fun EmbeddedPlayerDialog(
    source: PlayerSource,
    onProgress: (PlaybackPosition) -> Unit,
    onClose: (PlaybackPosition) -> Unit,
) {
    val ownerScope = rememberCoroutineScope()
    PlayerWindow(
        title = source.title,
    ) { window ->
        EmbeddedPlayerContent(
            source = source,
            window = window,
            onProgress = { position -> ownerScope.launch { onProgress(position) } },
            onClose = { position -> ownerScope.launch { onClose(position) } },
        )
    }
}
