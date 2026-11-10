@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class)

package app.deference.embycl.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import app.deference.coreui.LocalKeyShortcutManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.launch
import java.awt.Component
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

private val PIP_MINIMUM_SIZE = Dimension(320, 180)

/**
 * Drives fullscreen through the hosting [ComposeWindow]'s placement, so the host's
 * `WindowState` stays the source of truth. Leaving the player restores the placement
 * the host had when the player opened.
 */
@Composable
internal fun PlayerWindow(
    content: @Composable (PlayerWindowState) -> Unit,
) {
    val host = requireNotNull(value = LocalAwtWindow.current as? ComposeWindow) {
        "The desktop player must be hosted in a Compose Window."
    }
    val initialPlacement = remember(key1 = host) { host.placement }
    val window = remember(key1 = host) {
        var restorePlacement = initialPlacement.takeUnless { it == WindowPlacement.Fullscreen }
            ?: WindowPlacement.Maximized
        lateinit var state: PlayerWindowState
        val setFullscreen: (Boolean) -> Unit = { fullscreen ->
            val focusTarget = host.composeFocusTarget()
            val current = host.placement
            when {
                fullscreen && current != WindowPlacement.Fullscreen -> {
                    restorePlacement = current
                    host.placement = WindowPlacement.Fullscreen
                }
                !fullscreen && current == WindowPlacement.Fullscreen -> host.placement = restorePlacement
            }
            state.isFullscreen = fullscreen
            host.restoreFocus(target = focusTarget)
        }
        state = PlayerWindowState(
            supportsPictureInPicture = true,
            supportsFullscreen = true,
            pauseOnHostStop = false,
            togglePictureInPicture = {
                state.isPictureInPicture = !state.isPictureInPicture
                if (state.isPictureInPicture && state.isFullscreen) setFullscreen(false)
            },
            toggleFullscreen = { setFullscreen(!state.isFullscreen) },
        )
        state.isFullscreen = initialPlacement == WindowPlacement.Fullscreen
        state
    }
    DisposableEffect(key1 = host, key2 = window) {
        // Placement can also change outside the player, e.g. through the OS window manager.
        val placementListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                window.isFullscreen = host.placement == WindowPlacement.Fullscreen
            }
        }
        host.addComponentListener(placementListener)
        onDispose {
            host.removeComponentListener(placementListener)
            // Queue outside Compose's disposal stack; placement changes relayout synchronously.
            EventQueue.invokeLater {
                if (host.placement != initialPlacement) {
                    val focusTarget = host.composeFocusTarget()
                    host.placement = initialPlacement
                    host.restoreFocus(target = focusTarget)
                }
            }
        }
    }
    content(window)
}

/**
 * The AWT component that delivers key events to the Compose scene. Never the frame itself:
 * a focused frame swallows key events, so window-level shortcuts stop firing.
 */
private fun ComposeWindow.composeFocusTarget(): Component? =
    mostRecentFocusOwner?.takeUnless { it === this }
        ?: focusTraversalPolicy?.getDefaultComponent(this)

/**
 * Fullscreen goes through `GraphicsDevice.setFullScreenWindow`, and window managers may
 * deactivate the window during the transition. Focus is handed back to [target] once the
 * transition settles, or when the window is next activated if it is not active yet.
 */
private fun ComposeWindow.restoreFocus(target: Component?) {
    if (target == null) return
    EventQueue.invokeLater {
        if (isFocused) {
            target.requestFocusInWindow()
            return@invokeLater
        }
        addWindowFocusListener(
            object : WindowAdapter() {
                override fun windowGainedFocus(e: WindowEvent) {
                    removeWindowFocusListener(this)
                    target.requestFocusInWindow()
                }
            },
        )
        toFront()
        target.requestFocus()
    }
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
    val motion = MaterialTheme.motionScheme
    val videoColors = LocalPlayerVideoColorScheme.current
    val shortcuts = LocalKeyShortcutManager.current
    val ownerScope = rememberCoroutineScope()
    val requestedPip = window.isPictureInPicture
    var pipPresented by remember { mutableStateOf(value = false) }
    // Keep the outgoing host until the incoming host has actually drawn video.
    // The decoder and producer are outside both compositions and never restart.
    if (requestedPip || pipPresented) {
        // A decorated window: the system title bar moves it, so startMove stays a no-op.
        Window(
            onCloseRequest = { window.closeRequested = true },
            state = size,
            title = title,
            alwaysOnTop = true,
            resizable = true,
            // The mini window is a separate AWT window: route its keys to the same shortcuts as the main one.
            onPreviewKeyEvent = { event -> event.type == KeyEventType.KeyDown && shortcuts.processKeyEvent(event = event) },
        ) {
            LaunchedEffect(key1 = this.window) {
                this@Window.window.minimumSize = PIP_MINIMUM_SIZE
            }
            MaterialExpressiveTheme(
                colorScheme = colors,
                motionScheme = motion,
                typography = typography,
                shapes = shapes,
            ) {
                CompositionLocalProvider(
                    LocalKeyShortcutManager provides shortcuts,
                    LocalPlayerVideoColorScheme provides videoColors,
                ) {
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
    }
    if (!requestedPip || !pipPresented) {
        // The windowed layout draws its own margin and picture card; see PlayerOverlay.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colors.background),
        ) {
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                onDraw()
            },
    ) {
        content()
    }
}
