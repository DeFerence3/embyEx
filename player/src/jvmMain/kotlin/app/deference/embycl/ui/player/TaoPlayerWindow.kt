package app.deference.embycl.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.window.tao.LocalTaoWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlin.coroutines.EmptyCoroutineContext

@Composable
internal fun TaoPlayerWindow(
    content: @Composable (PlayerWindowState) -> Unit,
) {
    val parent = requireNotNull(value = LocalTaoWindow.current)
    val wasFullscreen = remember(key1 = parent) { parent.isFullscreen }
    val windowExecutor = remember(key1 = parent) {
        Executor { command -> Dispatchers.Main.dispatch(context = EmptyCoroutineContext, block = command) }
    }
    val window = remember(key1 = parent) {
        lateinit var state: PlayerWindowState
        state = PlayerWindowState(
            supportsPictureInPicture = true,
            supportsFullscreen = true,
            pauseOnHostStop = false,
            togglePictureInPicture = {
                state.isPictureInPicture = !state.isPictureInPicture
                windowExecutor.execute {
                    if (state.isPictureInPicture && parent.isFullscreen) {
                        parent.setFullscreen(fullscreen = false)
                        state.isFullscreen = false
                    }
                }
            },
            toggleFullscreen = {
                windowExecutor.execute {
                    val fullscreen = !parent.isFullscreen
                    parent.setFullscreen(fullscreen = fullscreen)
                    state.isFullscreen = fullscreen
                }
            },
        )
        state.isFullscreen = wasFullscreen
        state
    }
    DisposableEffect(key1 = parent) {
        onDispose {
            // Tao fullscreen changes render synchronously. Queue outside Compose's
            // current render/disposal stack, even when already on the Tao thread.
            windowExecutor.execute {
                if (parent.isFullscreen != wasFullscreen) {
                    parent.setFullscreen(fullscreen = wasFullscreen)
                }
            }
        }
    }
    content(window)
}

@Composable
internal fun TaoPlayerPresentation(
    title: String,
    window: PlayerWindowState,
    content: @Composable () -> Unit,
) {
    val size = rememberWindowState(
        width = 480.dp,
        height = 270.dp,
        position = WindowPosition(alignment = Alignment.BottomEnd),
    )
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    val ownerScope = rememberCoroutineScope()
    val requestedPip = window.isPictureInPicture
    var pipPresented by remember { mutableStateOf(value = false) }
    // Keep the outgoing host until the incoming host has actually drawn video.
    // The decoder and producer are outside both compositions and never restart.
    if (requestedPip || pipPresented) {
        DecoratedWindow(
            onCloseRequest = { window.closeRequested = true },
            state = size,
            title = title,
            alwaysOnTop = true,
            resizable = true,
            minimumSize = DpSize(width = 320.dp, height = 180.dp),
        ) {
            val nativeWindow = requireNotNull(value = LocalTaoWindow.current)
            DisposableEffect(key1 = nativeWindow, key2 = window) {
                window.startMove = { nativeWindow.dragWindow() }
                onDispose { window.startMove = {} }
            }
            MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes) {
                PlayerPresentationHost(
                    onDraw = {
                        if (window.isPictureInPicture && !pipPresented) {
                            ownerScope.launch { if (window.isPictureInPicture) pipPresented = true }
                        }
                    },
                    content = content,
                )
            }
        }
    }
    if (!requestedPip || !pipPresented) {
        PlayerInlay {
            PlayerPresentationHost(
                onDraw = {
                    if (!window.isPictureInPicture && pipPresented) {
                        ownerScope.launch { if (!window.isPictureInPicture) pipPresented = false }
                    }
                },
                content = content,
            )
        }
    }
}

@Composable
private fun PlayerPresentationHost(
    onDraw: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                onDraw()
            },
    ) {
        content()
    }
}
