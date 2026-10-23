package app.deference.embycl.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_close
import embyex.player.generated.resources.player_exit_fullscreen
import embyex.player.generated.resources.player_fullscreen
import embyex.player.generated.resources.player_pip
import embyex.player.generated.resources.player_pip_error
import embyex.player.generated.resources.player_restore
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.togglePlayWhenReady

@Composable
internal fun PlayerOverlay(
    player: MediampPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(value = true) }
    var interaction by remember { mutableIntStateOf(value = 0) }
    var pressed by remember { mutableStateOf(value = false) }
    var menuOpen by remember { mutableStateOf(value = false) }
    var controlsFocused by remember { mutableStateOf(value = false) }
    var keyboardInput by remember { mutableStateOf(value = false) }
    val focus = remember { FocusRequester() }
    val density = LocalDensity.current.density
    val accessibility = LocalAccessibilityManager.current
    val systemPip = window.isPictureInPicture && window.usesSystemPictureInPicture
    val keepVisible = paused || failed || player == null || pressed || menuOpen || (controlsFocused && keyboardInput)

    LaunchedEffect(key1 = paused, key2 = failed, key3 = window.isPictureInPicture) {
        visible = true
    }
    LaunchedEffect(key1 = interaction, key2 = keepVisible, key3 = visible) {
        if (visible && !keepVisible) {
            delay(timeMillis = accessibility?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = 3_000L,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) ?: 3_000L)
            visible = false
        }
    }
    LaunchedEffect(key1 = Unit) { focus.requestFocus() }
    LaunchedEffect(key1 = visible) {
        // A focused action can disappear with the controls. Keep shortcuts on
        // the player instead of letting focus fall back to the page underneath.
        if (!visible) focus.requestFocus()
    }

    Box(
        modifier = modifier
            .pointerInput(key1 = Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type != PointerEventType.Exit) {
                            interaction++
                            keyboardInput = false
                        }
                        if (event.type == PointerEventType.Move && event.changes.any {
                            it.type == PointerType.Mouse && !it.pressed && it.position != it.previousPosition
                        }) {
                            visible = true
                        }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .pointerInput(key1 = Unit) {
                // Child buttons and sliders consume their gestures before this parent.
                detectTapGestures(onTap = {
                    focus.requestFocus()
                    visible = !visible
                })
            }
            .onPreviewKeyEvent { event ->
                interaction++
                keyboardInput = true
                visible = true
                if (event.key == Key.F && window.supportsFullscreen && !window.isPictureInPicture) {
                    // Toggle once per press; held keys can repeat KeyDown.
                    if (event.type == KeyEventType.KeyUp) window.toggleFullscreen()
                    event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp
                } else if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.Escape -> {
                        when {
                            window.isPictureInPicture && !systemPip -> window.togglePictureInPicture()
                            window.isFullscreen && window.supportsFullscreen -> window.toggleFullscreen()
                            else -> onClose()
                        }
                        true
                    }
                    Key.Spacebar -> if (!controlsFocused && player != null && !failed) {
                        dispatchAction { player.togglePlayWhenReady() }
                        true
                    } else false
                    else -> false
                }
            }
            .focusRequester(focusRequester = focus)
            .focusable(),
    ) {
        // Drag the video area to move the undecorated desktop window; controls handle their own gestures.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(key1 = window, key2 = density) {
                    detectDragGestures(
                        onDragStart = { window.startMove() },
                        onDrag = { change, amount ->
                            change.consume()
                            window.moveBy(amount.x / density, amount.y / density)
                        },
                    )
                },
        )
        CompositionLocalProvider(LocalContentColor provides PlayerColors.content) {
            AnimatedVisibility(
                visible = visible && !systemPip,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { controlsFocused = it.hasFocus }
                        .focusGroup(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (player != null) {
                            PlayerTrackControls(
                                player = player,
                                enabled = !failed,
                                onMenuExpandedChange = { menuOpen = it },
                                dispatchAction = dispatchAction,
                            )
                        }
                        if (window.supportsPictureInPicture) {
                            PlayerIconButton(
                                icon = if (window.isPictureInPicture) Icons.Default.OpenInFull else Icons.Default.PictureInPictureAlt,
                                description = stringResource(resource = if (window.isPictureInPicture) Res.string.player_restore else Res.string.player_pip),
                                onClick = window.togglePictureInPicture,
                            )
                        }
                        if (window.supportsFullscreen && !window.isPictureInPicture) {
                            PlayerIconButton(
                                icon = if (window.isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                description = stringResource(resource = if (window.isFullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen),
                                onClick = window.toggleFullscreen,
                            )
                        }
                        PlayerIconButton(
                            icon = Icons.Default.Close,
                            description = stringResource(resource = Res.string.player_close),
                            onClick = onClose,
                        )
                    }
                    Column {
                        if (window.pictureInPictureFailed) {
                            Text(
                                text = stringResource(resource = Res.string.player_pip_error),
                                modifier = Modifier.padding(all = 12.dp),
                            )
                        }
                        if (player != null) PlayerControls(player = player, enabled = !failed, dispatchAction = dispatchAction)
                    }
                }
            }
        }
    }
}
