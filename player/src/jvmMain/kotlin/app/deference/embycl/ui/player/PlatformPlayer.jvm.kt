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
        engine = MpvPlayer(
            coroutineContext = coroutineContext,
            dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher,
        ),
        scope = CoroutineScope(context = coroutineContext),
    )
} catch (_: LinkageError) {
    throw IllegalStateException("The embedded player runtime could not be loaded.")
}

internal actual fun createPlatformMediaSession(
    context: Any,
    source: PlayerSource,
    player: EmbeddedPlayer,
    dispatchAction: (() -> Unit) -> Unit,
    onStop: () -> Unit,
): PlatformMediaSession? {
    // Linux publishes through MPRIS. Windows' System Media Transport Controls are not implemented.
    if (!System.getProperty("os.name").startsWith("Linux", ignoreCase = true)) return null
    return try {
        MprisMediaSession(
            source = source,
            player = player,
            dispatchAction = dispatchAction,
            stopPlayback = onStop,
        )
    } catch (_: LinkageError) {
        null
    }
}

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: EmbeddedPlayer,
    modifier: Modifier,
) {
    require(value = player is JvmEmbeddedPlayer)
    PlayerSurface(player = player.engine, modifier = modifier)
}

@Composable
internal actual fun PlayerWindow(
    title: String,
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    PlayerWindow(content = content)
}

@Composable
internal actual fun PlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
) {
    TaoPlayerPresentation(title = title, window = window, content = content)
}
