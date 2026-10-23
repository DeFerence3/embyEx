package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.mpv.MPVHandle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher

@Composable
internal actual fun playerContext(): Any = Unit

internal actual fun isRecreatingPlayerHost(context: Any): Boolean = false

internal actual fun createEmbeddedPlayer(
    context: Any,
    coroutineContext: CoroutineContext,
): MediampPlayer = try {
    // Native errors may include authenticated URLs. UI shows sanitized errors instead.
    MPVHandle.setLogHandler(handler = { })
    TaoMpvPlayer(
        coroutineContext = coroutineContext,
        dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
    )
} catch (_: LinkageError) {
    throw IllegalStateException("The embedded player runtime could not be loaded.")
}

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: MediampPlayer,
    modifier: Modifier,
) {
    TaoPlayerSurface(player = player as TaoMpvPlayer, modifier = modifier)
}

@Composable
internal actual fun PlayerWindow(
    title: String,
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    TaoPlayerWindow(content = content)
}

@Composable
internal actual fun PlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
) {
    TaoPlayerPresentation(title = title, window = window, content = content)
}
