package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.exoplayer.ExoPlayerMediampPlayer
import org.openani.mediamp.exoplayer.ExoPlayerMediampPlayerFactory
import org.openani.mediamp.exoplayer.compose.ExoPlayerMediampPlayerSurface
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
): MediampPlayer = ExoPlayerMediampPlayerFactory().create(
    context = context,
    parentCoroutineContext = coroutineContext,
)

@Composable
internal actual fun EmbeddedPlayerSurface(
    player: MediampPlayer,
    modifier: Modifier,
) {
    require(value = player is ExoPlayerMediampPlayer)
    val view = LocalView.current
    DisposableEffect(key1 = view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    ExoPlayerMediampPlayerSurface(
        mediampPlayer = player,
        modifier = modifier,
    )
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
