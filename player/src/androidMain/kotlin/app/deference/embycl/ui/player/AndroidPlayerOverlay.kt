package app.deference.embycl.ui.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.components.AndroidPlayerButton
import app.deference.embycl.ui.player.components.AndroidPlayerAspectButton
import app.deference.embycl.ui.player.components.AndroidPlayerAspectFeedback
import app.deference.embycl.ui.player.components.AndroidPlayerLevel
import app.deference.embycl.ui.player.components.AndroidPlayerSheets
import app.deference.embycl.ui.player.components.AndroidPlayerSeekFeedback
import app.deference.embycl.ui.player.components.AndroidPlayerTimeline
import app.deference.embycl.ui.player.components.AndroidPlayerTitle
import app.deference.embycl.ui.player.components.AndroidPlayerUnlock
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_lock
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_pause
import embyex.player.generated.resources.player_pip
import embyex.player.generated.resources.player_pip_error
import embyex.player.generated.resources.player_play
import embyex.player.generated.resources.player_rotate
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_subtitles
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.features.AspectRatioMode
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.features.VideoAspectRatio
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.togglePlayWhenReady

@Composable
internal fun AndroidPlayerOverlay(
    title: String,
    player: MediampPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.playerActivity()
    val accessibility = LocalAccessibilityManager.current
    var visible by remember { mutableStateOf(value = true) }
    var locked by remember { mutableStateOf(value = false) }
    var pressed by remember { mutableStateOf(value = false) }
    var seeking by remember { mutableStateOf(value = false) }
    var interaction by remember { mutableIntStateOf(value = 0) }
    var sheet by remember { mutableStateOf<AndroidPlayerSheet?>(value = null) }
    var feedback by remember { mutableStateOf<AndroidPlayerFeedback?>(value = null) }
    var feedbackVersion by remember { mutableIntStateOf(value = 0) }
    var seekSeconds by remember { mutableIntStateOf(value = 0) }
    var seekVersion by remember { mutableIntStateOf(value = 0) }
    var aspectFeedback by remember { mutableStateOf<AspectRatioMode?>(value = null) }
    var aspectVersion by remember { mutableIntStateOf(value = 0) }
    val keepVisible = (!locked && (paused || failed || player == null)) || pressed || seeking || sheet != null
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
    LaunchedEffect(key1 = aspectVersion) {
        if (aspectFeedback != null) {
            delay(timeMillis = 1_500L)
            aspectFeedback = null
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
                        pressed = event.changes.any { it.pressed }
                    }
                }
            },
    ) {
        val portrait = maxHeight > maxWidth
        val rotate: () -> Unit = {
            activity.requestedOrientation = if (portrait) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
        }
        AndroidPlayerGestures(
            player = player,
            enabled = !failed && !locked,
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
            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = Brush.verticalGradient(
                        0f to PlayerColors.controlsScrim,
                        0.4f to PlayerColors.transparent,
                        0.6f to PlayerColors.transparent,
                        1f to PlayerColors.controlsScrim,
                    ))
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = if (portrait) 24.dp else 8.dp),
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
                    if (!portrait) AndroidPlayerTrackActions(player = player, enabled = !failed, onSheet = onSheet)
                }
                if (!portrait) AndroidPlayerPlayButton(
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
                    verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    if (window.pictureInPictureFailed) Text(
                        text = stringResource(resource = Res.string.player_pip_error),
                        color = PlayerColors.content,
                    )
                    if (portrait && player != null) AndroidPlayerTimeline(
                        player = player,
                        enabled = !failed,
                        onSeeking = { seeking = it },
                        dispatchAction = dispatchAction,
                    )
                    if (portrait) AndroidPlayerPlayButton(
                        player = player,
                        paused = paused,
                        enabled = !failed,
                        dispatchAction = dispatchAction,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(state = rememberScrollState()),
                        horizontalArrangement = if (portrait) Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally)
                            else Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                            AndroidPlayerButton(
                                icon = Icons.Default.ScreenRotation,
                                description = stringResource(resource = Res.string.player_rotate),
                                onClick = rotate,
                            )
                            if (portrait) AndroidPlayerTrackActions(
                                player = player,
                                enabled = !failed,
                                onSheet = onSheet,
                                showMore = false,
                            )
                            if (player?.features?.get(PlaybackSpeed) != null) AndroidPlayerButton(
                                icon = Icons.Default.Speed,
                                description = stringResource(resource = Res.string.player_speed),
                                onClick = { onSheet(AndroidPlayerSheet.Speed) },
                                enabled = !failed,
                            )
                            if (!portrait) AndroidPlayerButton(
                                icon = Icons.Default.Lock,
                                description = stringResource(resource = Res.string.player_lock),
                                onClick = { locked = true },
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                            player?.features?.get(VideoAspectRatio)?.let { aspect ->
                                AndroidPlayerAspectButton(
                                    aspect = aspect,
                                    enabled = !failed,
                                    onModeChanged = { aspectFeedback = it; aspectVersion++ },
                                    dispatchAction = dispatchAction,
                                )
                            }
                            if (window.supportsPictureInPicture) AndroidPlayerButton(
                                icon = Icons.Default.PictureInPictureAlt,
                                description = stringResource(resource = Res.string.player_pip),
                                onClick = window.togglePictureInPicture,
                            )
                            if (portrait) {
                                AndroidPlayerButton(
                                    icon = Icons.Default.Lock,
                                    description = stringResource(resource = Res.string.player_lock),
                                    onClick = { locked = true },
                                )
                                AndroidPlayerButton(
                                    icon = Icons.Default.MoreVert,
                                    description = stringResource(resource = Res.string.player_more),
                                    onClick = { onSheet(AndroidPlayerSheet.More) },
                                    enabled = player != null,
                                )
                            }
                        }
                    }
                    if (!portrait && player != null) AndroidPlayerTimeline(
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
        AndroidPlayerAspectFeedback(
            mode = aspectFeedback,
            trigger = aspectVersion,
            modifier = Modifier
                .align(alignment = Alignment.TopCenter)
                .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                .padding(top = if (portrait) 96.dp else 72.dp),
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
    }
    val currentSheet = sheet
    if (currentSheet != null && player != null) AndroidPlayerSheets(
        sheet = currentSheet,
        player = player,
        onDismiss = { sheet = null; visible = true; interaction++ },
        onSheet = onSheet,
        dispatchAction = dispatchAction,
    )
}

@Composable
private fun AndroidPlayerTrackActions(
    player: MediampPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    showMore: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        if (player?.audioTracks != null) AndroidPlayerButton(
            icon = Icons.Default.Audiotrack,
            description = stringResource(resource = Res.string.player_audio),
            onClick = { onSheet(AndroidPlayerSheet.Audio) },
            enabled = enabled,
        )
        if (player?.subtitleTracks != null) AndroidPlayerButton(
            icon = Icons.Default.Subtitles,
            description = stringResource(resource = Res.string.player_subtitles),
            onClick = { onSheet(AndroidPlayerSheet.Subtitles) },
            enabled = enabled,
        )
        if (showMore) AndroidPlayerButton(
            icon = Icons.Default.MoreVert,
            description = stringResource(resource = Res.string.player_more),
            onClick = { onSheet(AndroidPlayerSheet.More) },
            enabled = player != null,
        )
    }
}

@Composable
private fun AndroidPlayerPlayButton(
    player: MediampPlayer?,
    paused: Boolean,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = player?.state?.collectAsState()?.value
    if (state?.isLoadingOrBuffering == true) {
        CircularProgressIndicator(modifier = modifier.size(size = 64.dp), color = PlayerColors.content)
    } else {
        AndroidPlayerButton(
            icon = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
            description = stringResource(resource = if (paused) Res.string.player_play else Res.string.player_pause),
            onClick = { player?.let { dispatchAction { it.togglePlayWhenReady() } } },
            enabled = enabled && player != null,
            size = 64.dp,
            modifier = modifier,
        )
    }
}
