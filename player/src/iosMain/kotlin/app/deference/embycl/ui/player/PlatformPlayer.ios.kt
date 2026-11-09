package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.coroutines.CoroutineContext

@Composable
internal actual fun playerContext(): Any = Unit

internal actual fun isRecreatingPlayerHost(context: Any): Boolean = false

internal actual fun createEmbeddedPlayer(
    context: Any,
    coroutineContext: CoroutineContext,
): EmbeddedPlayer = throw UnsupportedOperationException("Embedded playback is not configured on iOS.")

internal actual fun createPlatformMediaSession(
    context: Any,
    source: PlayerSource,
    player: EmbeddedPlayer,
    dispatchAction: (() -> Unit) -> Unit,
    onStop: () -> Unit,
): PlatformMediaSession? = null

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: EmbeddedPlayer,
    modifier: Modifier,
) = Unit

@Composable
internal actual fun PlayerWindow(
    title: String,
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    val window = remember { PlayerWindowState() }
    content(window)
}

@Composable
internal actual fun PlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
) = content()
