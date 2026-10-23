package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.openani.mediamp.MediampPlayer
import kotlin.coroutines.CoroutineContext

@Composable
internal expect fun playerContext(): Any

internal expect fun isRecreatingPlayerHost(context: Any): Boolean

internal expect fun createEmbeddedPlayer(
    context: Any,
    coroutineContext: CoroutineContext,
): MediampPlayer

@Composable
internal expect fun EmbeddedPlayerSurface(
    player: MediampPlayer,
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
    title: String,
    player: MediampPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
)
