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

/**
 * Publishes [player], playing [source], to the system media controls, or returns null where the
 * platform has none. [source] supplies what does not change during playback, such as the artwork.
 *
 * Commands from the system run through [dispatchAction]; [onStop] asks the host to close the
 * player. May throw: media controls are optional, so callers continue without them.
 */
internal expect fun createPlatformMediaSession(
    context: Any,
    source: PlayerSource,
    player: EmbeddedPlayer,
    dispatchAction: (() -> Unit) -> Unit,
    onStop: () -> Unit,
): PlatformMediaSession?

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
