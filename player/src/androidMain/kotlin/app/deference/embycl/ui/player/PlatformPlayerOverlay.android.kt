package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.openani.mediamp.MediampPlayer

@Composable
internal actual fun PlatformPlayerOverlay(
    title: String,
    player: MediampPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier,
) {
    AndroidPlayerOverlay(
        title = title,
        player = player,
        window = window,
        paused = paused,
        failed = failed,
        onClose = onClose,
        dispatchAction = dispatchAction,
        modifier = modifier,
    )
}
