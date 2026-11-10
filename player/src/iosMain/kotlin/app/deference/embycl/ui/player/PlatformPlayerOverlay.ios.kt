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
    queue: PlayerQueueControls?,
    subtitleStyle: PlayerSubtitleStyleControls,
    surface: @Composable (Modifier) -> Unit,
    modifier: Modifier,
) {
    PlayerOverlay(
        source = source,
        player = player,
        window = window,
        paused = paused,
        failed = failed,
        onClose = onClose,
        dispatchAction = dispatchAction,
        queue = queue,
        subtitleStyle = subtitleStyle,
        surface = surface,
        modifier = modifier,
    )
}
