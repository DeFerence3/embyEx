package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.deference.embycl.ui.player.EmbeddedPlayer
import kotlin.coroutines.CoroutineContext

@Composable
internal expect fun playerContext(): Any

internal expect fun isRecreatingPlayerHost(context: Any): Boolean

internal expect fun createEmbeddedPlayer(
    context: Any,
    coroutineContext: CoroutineContext,
): EmbeddedPlayer

@Composable
internal expect fun EmbeddedPlayerSurface(
    player: EmbeddedPlayer,
    modifier: Modifier = Modifier,
)

@Composable
internal expect fun PlayerWindow(
    title: String,
    content: @Composable (window: PlayerWindowState) -> Unit,
)

@Composable
internal expect fun PlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
)

@Composable
internal expect fun PlatformPlayerOverlay(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
)
