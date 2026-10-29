package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
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
): EmbeddedPlayer = try {
    // Native errors may include authenticated URLs. UI shows sanitized errors instead.
    MPVHandle.setLogHandler(handler = { })
    JvmEmbeddedPlayer(
        engine = TaoMpvPlayer(
            coroutineContext = coroutineContext,
            dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        ),
        scope = CoroutineScope(context = coroutineContext),
    )
} catch (_: LinkageError) {
    throw IllegalStateException("The embedded player runtime could not be loaded.")
}

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: EmbeddedPlayer,
    modifier: Modifier,
) {
    require(value = player is JvmEmbeddedPlayer)
    TaoPlayerSurface(player = player.engine, modifier = modifier)
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
