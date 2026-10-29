package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformPlayerOverlay(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier,
) {
    PlayerOverlay(
        title = source.title,
        player = player,
        window = window,
        paused = paused,
        failed = failed,
        onClose = onClose,
        dispatchAction = dispatchAction,
        modifier = modifier,
    )
}
