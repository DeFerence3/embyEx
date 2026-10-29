package app.deference.embycl.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.components.PlayerFullScreenControls
import app.deference.embycl.ui.player.components.PlayerTimeline
import app.deference.embycl.ui.player.components.PlayerTransportControls
import app.deference.embycl.ui.player.components.PlayerWindowControls
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_exit_fullscreen
import embyex.player.generated.resources.player_fullscreen
import embyex.player.generated.resources.player_pip_error
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlayerOverlay(
    title: String,
    player: EmbeddedPlayer?,
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
    var seeking by remember { mutableStateOf(value = false) }
    var controlsFocused by remember { mutableStateOf(value = false) }
    var keyboardInput by remember { mutableStateOf(value = false) }
    val focus = remember { FocusRequester() }
    val density = LocalDensity.current.density
    val accessibility = LocalAccessibilityManager.current
    val systemPip = window.isPictureInPicture && window.usesSystemPictureInPicture
    val fullControls = !window.isPictureInPicture
    val fullscreenDescription = stringResource(
        resource = if (window.isFullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen,
    )
    val keepVisible = !fullControls || paused || failed || player == null || pressed || seeking || menuOpen || (controlsFocused && keyboardInput)

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
    LaunchedEffect(key1 = fullControls) {
        visible = true
        menuOpen = false
        seeking = false
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
            .pointerInput(window, fullControls, window.isPictureInPicture) {
                // Child buttons and sliders consume their gestures before this parent.
                detectTapGestures(
                    onTap = {
                        focus.requestFocus()
                        visible = !fullControls || !visible
                    },
                    onDoubleTap = {
                        if (window.supportsFullscreen && !window.isPictureInPicture) window.toggleFullscreen()
                    },
                )
            }
            .semantics {
                if (window.supportsFullscreen && !window.isPictureInPicture) {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = fullscreenDescription,
                            action = { window.toggleFullscreen(); true },
                        ),
                    )
                }
            }
            .onPreviewKeyEvent { event ->
                if (menuOpen) return@onPreviewKeyEvent false
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
        if (window.isPictureInPicture && !systemPip) Box(
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
                visible = (visible || !fullControls) && !systemPip,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onFocusChanged { controlsFocused = it.hasFocus }
                        .focusGroup()
                        .background(
                            brush = Brush.verticalGradient(
                                0f to PlayerColors.controlsScrim,
                                0.4f to PlayerColors.transparent,
                                0.6f to PlayerColors.transparent,
                                1f to PlayerColors.controlsScrim,
                            ),
                        )
                        .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                        .padding(all = if (fullControls) 16.dp else 0.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (fullControls) {
                        PlayerFullScreenControls(
                            title = title,
                            player = player,
                            enabled = !failed,
                            onMenuExpandedChange = { menuOpen = it; interaction++ },
                            dispatchAction = dispatchAction,
                            onClose = onClose,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayerTransportControls(
                                player = player,
                                paused = paused,
                                enabled = !failed,
                                dispatchAction = dispatchAction,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (window.pictureInPictureFailed) {
                            Text(
                                text = stringResource(resource = Res.string.player_pip_error),
                                modifier = Modifier.padding(all = 12.dp),
                            )
                        }
                        if (fullControls) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                            ) {
                                Box(modifier = Modifier.weight(weight = 1f)) {
                                    if (player != null) PlayerTimeline(
                                        player = player,
                                        enabled = !failed,
                                        onSeeking = { seeking = it },
                                        dispatchAction = dispatchAction,
                                    )
                                }
                                PlayerWindowControls(
                                    fullscreen = window.isFullscreen,
                                    onToggleFullscreen = if (window.supportsFullscreen) window.toggleFullscreen else null,
                                    onPictureInPicture = if (window.supportsPictureInPicture) window.togglePictureInPicture else null,
                                )
                            }
                        } else if (player != null) {
                            PlayerControls(
                                player = player,
                                enabled = !failed,
                                dispatchAction = dispatchAction,
                                showTime = fullControls,
                                pictureInPicture = window.isPictureInPicture,
                                onPictureInPicture = if (!fullControls && window.supportsPictureInPicture) window.togglePictureInPicture else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
