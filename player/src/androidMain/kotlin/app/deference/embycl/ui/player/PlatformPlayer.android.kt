package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import app.deference.embycl.ui.player.EmbeddedPlayer
import androidx.compose.runtime.key
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@Composable
internal actual fun playerContext(): Any = LocalContext.current

internal actual fun isRecreatingPlayerHost(context: Any): Boolean {
    var current = context
    while (current is ContextWrapper && current !is Activity) current = current.baseContext
    return (current as? Activity)?.isChangingConfigurations == true
}

internal actual fun createEmbeddedPlayer(
    context: Any,
    coroutineContext: CoroutineContext,
): EmbeddedPlayer = AndroidMpvPlayer(
    context = context as Context,
    ownerScope = CoroutineScope(context = coroutineContext),
)

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: EmbeddedPlayer,
    modifier: Modifier,
) {
    require(value = player is AndroidMpvPlayer)
    val view = LocalView.current
    DisposableEffect(key1 = view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    key(player) {
        AndroidView(
            factory = { context -> AndroidMpvSurfaceView(context = context, player = player) },
            modifier = modifier,
            onReset = null,
            onRelease = { it.release() },
        )
    }
}

@Composable
internal actual fun PlayerWindow(
    title: String,
    content: @Composable (window: PlayerWindowState) -> Unit,
) {
    AndroidPlayerWindow(content = content)
}

@Composable
internal actual fun PlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
) = content()
