package app.deference.embycl.ui.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.components.AndroidPlayerLevel
import app.deference.embycl.ui.player.components.AndroidPlayerOptionActions
import app.deference.embycl.ui.player.components.AndroidPlayerQuickActions
import app.deference.embycl.ui.player.components.AndroidPlayerSeekFeedback
import app.deference.embycl.ui.player.components.AndroidPlayerSheets
import app.deference.embycl.ui.player.components.AndroidPlayerTitle
import app.deference.embycl.ui.player.components.AndroidPlayerTrackActions
import app.deference.embycl.ui.player.components.AndroidPlayerUnlock
import app.deference.embycl.ui.player.components.AndroidPlayerUtilityActions
import app.deference.embycl.ui.player.components.PlayerTimeline
import app.deference.embycl.ui.player.components.PlayerTransportControls
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_pip_error
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AndroidPlayerOverlay(
    title: String,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    onOpenExternalPlayer: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.playerActivity()
    val accessibility = LocalAccessibilityManager.current
    var visible by remember { mutableStateOf(value = true) }
    var locked by remember { mutableStateOf(value = false) }
    var pressed by remember { mutableStateOf(value = false) }
    var controlsFocused by remember { mutableStateOf(value = false) }
    var keyboardInput by remember { mutableStateOf(value = false) }
    val focus = remember { FocusRequester() }
    var seeking by remember { mutableStateOf(value = false) }
    var interaction by remember { mutableIntStateOf(value = 0) }
    var sheet by remember { mutableStateOf<AndroidPlayerSheet?>(value = null) }
    var moreExpanded by rememberSaveable { mutableStateOf(value = false) }
    var feedback by remember { mutableStateOf<AndroidPlayerFeedback?>(value = null) }
    var feedbackVersion by remember { mutableIntStateOf(value = 0) }
    var seekSeconds by remember { mutableIntStateOf(value = 0) }
    var seekVersion by remember { mutableIntStateOf(value = 0) }
    val keepVisible = (!locked && (paused || failed || player == null)) || pressed || seeking ||
        sheet != null || moreExpanded || window.isPictureInPicture || (controlsFocused && keyboardInput)
    LaunchedEffect(key1 = visible, key2 = window.isPictureInPicture) {
        if (!visible && sheet == null && !window.isPictureInPicture) focus.requestFocus()
    }
    DisposableEffect(key1 = activity) {
        val orientation = activity.requestedOrientation
        onDispose { activity.requestedOrientation = orientation }
    }
    LaunchedEffect(key1 = paused, key2 = failed, key3 = window.isPictureInPicture) { visible = true }
    LaunchedEffect(visible, keepVisible, interaction, locked) {
        if (visible && !keepVisible) {
            delay(timeMillis = accessibility?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = if (locked) 2_000L else 3_000L,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) ?: 3_000L)
            visible = false
        }
    }
    LaunchedEffect(key1 = feedbackVersion) {
        if (feedback != null) {
            delay(timeMillis = 1_300L)
            feedback = null
        }
    }
    LaunchedEffect(key1 = seekVersion) {
        if (seekSeconds != 0) {
            delay(timeMillis = 1_300L)
            seekSeconds = 0
        }
    }
    BackHandler(enabled = locked && !window.isPictureInPicture) { visible = true }
    if (window.isPictureInPicture) return
    val onSheet: (AndroidPlayerSheet) -> Unit = { sheet = it; visible = true }
    BoxWithConstraints(
        modifier = modifier
            .pointerInput(key1 = Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        interaction++
                        keyboardInput = false
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            /*.onPreviewKeyEvent { event ->
                keyboardInput = true
                visible = true
                interaction++
				event.type == KeyEventType.KeyDown && !locked && sheet == null && when (event.key) {
					Key.Escape -> { onClose(); true }
					Key.Spacebar -> if (!controlsFocused && player != null && !failed) {
						dispatchAction { player.togglePlayWhenReady() }
						true
					} else false
					else -> false
				}
            }*/
            .focusRequester(focusRequester = focus)
            .focusable(),
    ) {
        val portrait = maxHeight > maxWidth
        val moreOptions: @Composable () -> Unit = {
            AndroidPlayerOptionActions(
                portrait = portrait,
                expanded = moreExpanded,
                onExpandedChange = {
                    moreExpanded = it
                    visible = true
                    interaction++
                },
                onOpenExternalPlayer = onOpenExternalPlayer,
            )
        }
        val rotate: () -> Unit = {
            activity.requestedOrientation = if (portrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
        val lockControls: () -> Unit = { locked = true; visible = true }
        val enterPictureInPicture: (() -> Unit)? = if (window.supportsPictureInPicture) {
            { window.togglePictureInPicture() }
        } else null
        AndroidPlayerGestures(
            player = player,
            enabled = !failed && !locked && !moreExpanded,
            onTap = { visible = !visible },
            onSeek = { seconds ->
                seekSeconds = if ((seekSeconds > 0) == (seconds > 0)) seekSeconds + seconds else seconds
                seekVersion++
            },
            onFeedback = { feedback = it; feedbackVersion++ },
            onDragging = { seeking = it },
            dispatchAction = dispatchAction,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = visible && !locked && sheet == null,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { controlsFocused = it.hasFocus }
                    .focusGroup()
                    .background(brush = Brush.verticalGradient(
                        0f to PlayerColors.controlsScrim,
                        0.4f to PlayerColors.transparent,
                        0.6f to PlayerColors.transparent,
                        1f to PlayerColors.controlsScrim,
                    ))
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = if (portrait) 16.dp else 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    AndroidPlayerTitle(
                        title = title,
                        onClose = onClose,
                        modifier = Modifier.weight(weight = 1f),
                    )

                    if (!portrait) AndroidPlayerTrackActions(
                        player = player,
                        enabled = !failed,
                        onSheet = onSheet,
                        moreOptions = moreOptions,
                    )
                }
                if (!portrait) PlayerTransportControls(
                    player = player,
                    paused = paused,
                    enabled = !failed,
                    dispatchAction = dispatchAction,
                    modifier = Modifier.align(alignment = Alignment.Center),
                )
                Column(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space = if (portrait) 24.dp else 8.dp),
                ) {
                    if (window.pictureInPictureFailed) Text(
                        text = stringResource(resource = Res.string.player_pip_error),
                        color = PlayerColors.content,
                    )
                    if (portrait && player != null) PlayerTimeline(
                        player = player,
                        enabled = !failed,
                        onSeeking = { seeking = it },
                        dispatchAction = dispatchAction,
                    )
                    if (portrait) PlayerTransportControls(
                        player = player,
                        paused = paused,
                        enabled = !failed,
                        dispatchAction = dispatchAction,
                    )
                    if (portrait) AndroidPlayerQuickActions(
                        player = player,
                        enabled = !failed,
                        onSheet = onSheet,
                        onRotate = rotate,
                        onLock = lockControls,
                        onPictureInPicture = enterPictureInPicture,
                        moreOptions = moreOptions,
                        modifier = Modifier.fillMaxWidth(),
                    ) else AndroidPlayerUtilityActions(
                        player = player,
                        enabled = !failed,
                        onSheet = onSheet,
                        onRotate = rotate,
                        onLock = lockControls,
                        onPictureInPicture = enterPictureInPicture,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!portrait && player != null) PlayerTimeline(
                        player = player,
                        enabled = !failed,
                        onSeeking = { seeking = it },
                        dispatchAction = dispatchAction,
                    )
                }
            }
        }
        if (locked && visible) AndroidPlayerUnlock(
            onUnlock = { locked = false; visible = true },
            onDragging = { seeking = it },
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                .padding(bottom = 24.dp),
        )
        AndroidPlayerSeekFeedback(
            seconds = seekSeconds,
            trigger = seekVersion,
            modifier = Modifier.fillMaxSize(),
        )
        feedback?.let { update ->
            if (update.level != null) AndroidPlayerLevel(
                feedback = update,
                modifier = Modifier
                    .align(alignment = if (update.onLeft) Alignment.CenterStart else Alignment.CenterEnd)
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp),
            ) else Surface(
                modifier = Modifier.align(alignment = Alignment.Center),
                shape = MaterialTheme.shapes.large,
                color = PlayerColors.controlsSurface,
                contentColor = PlayerColors.content,
            ) {
                Text(text = update.message, modifier = Modifier.padding(all = 20.dp))
            }
        }
        val currentSheet = sheet
        if (currentSheet != null && player != null) AndroidPlayerSheets(
            sheet = currentSheet,
            player = player,
            onDismiss = { sheet = null; visible = true; interaction++ },
            dispatchAction = dispatchAction,
        )
    }
}
