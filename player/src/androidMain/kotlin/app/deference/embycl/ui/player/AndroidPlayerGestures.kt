package app.deference.embycl.ui.player

import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import app.deference.embycl.ui.player.components.androidPlayerTime
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_brightness
import embyex.player.generated.resources.player_volume
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.togglePlayWhenReady
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun AndroidPlayerGestures(
    player: MediampPlayer?,
    enabled: Boolean,
    onTap: () -> Unit,
    onSeek: (seconds: Int) -> Unit,
    onFeedback: (AndroidPlayerFeedback) -> Unit,
    onDragging: (Boolean) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.playerActivity()
    val audio = remember(key1 = activity) { activity.getSystemService(AudioManager::class.java) }
    val currentTap by rememberUpdatedState(newValue = onTap)
    val currentSeek by rememberUpdatedState(newValue = onSeek)
    val currentFeedback by rememberUpdatedState(newValue = onFeedback)
    val currentDragging by rememberUpdatedState(newValue = onDragging)
    val currentDispatch by rememberUpdatedState(newValue = dispatchAction)
    val volumeLabel = stringResource(resource = Res.string.player_volume)
    val brightnessLabel = stringResource(resource = Res.string.player_brightness)
    DisposableEffect(key1 = activity) {
        val brightness = activity.window.attributes.screenBrightness
        onDispose {
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = brightness }
        }
    }
    Box(
        modifier = modifier
            .pointerInput(key1 = player, key2 = enabled) {
                detectTapGestures(
                    onTap = { currentTap() },
                    onDoubleTap = { position ->
                        if (enabled && player?.state?.value?.mediaStatus == MediaStatus.Ready) {
                            when {
                                position.x < size.width * 0.35f -> {
                                    currentDispatch { player.skip(deltaMillis = -10_000L) }
                                    currentSeek(-10)
                                }
                                position.x > size.width * 0.65f -> {
                                    currentDispatch { player.skip(deltaMillis = 10_000L) }
                                    currentSeek(10)
                                }
                                else -> currentDispatch { player.togglePlayWhenReady() }
                            }
                        } else currentTap()
                    },
                )
            }
            .pointerInput(key1 = player, key2 = enabled) {
                if (!enabled || player == null) return@pointerInput
                var origin = Offset.Zero
                var total = Offset.Zero
                var horizontal: Boolean? = null
                var position = 0L
                var seekTarget = 0L
                var initialLevel = 0f
                val maximumVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val percent = NumberFormat.getPercentInstance()
                detectDragGestures(
                    onDragStart = { point ->
                        origin = point
                        total = Offset.Zero
                        horizontal = null
                        position = player.currentPositionMillis.value
                        seekTarget = position
                        initialLevel = if (point.x < size.width / 2f) {
                            audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maximumVolume
                        } else {
                            activity.window.attributes.screenBrightness.takeIf { it >= 0f }
                                ?: (Settings.System.getInt(
                                    activity.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    128,
                                ) / 255f)
                        }
                        currentDragging(true)
                    },
                    onDragEnd = {
                        if (horizontal == true && player.state.value.mediaStatus == MediaStatus.Ready) {
                            val target = seekTarget
                            currentDispatch { player.seekTo(positionMillis = target) }
                        }
                        currentDragging(false)
                    },
                    onDragCancel = { currentDragging(false) },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        if (horizontal == null) horizontal = abs(total.x) > abs(total.y)
                        if (horizontal == true) {
                            val duration = player.mediaProperties.value?.durationMillis ?: 0L
                            if (duration > 0L) {
                                seekTarget = (position + total.x / size.width * 120_000L).toLong().coerceIn(0L, duration)
                                currentFeedback(AndroidPlayerFeedback(message = androidPlayerTime(milliseconds = seekTarget)))
                            }
                        } else {
                            val value = (initialLevel - total.y / size.height * 2f).coerceIn(0f, 1f)
                            if (origin.x < size.width / 2f) {
                                if (!audio.isVolumeFixed) {
                                    val index = (value * maximumVolume).roundToInt()
                                    if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) != index) {
                                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
                                    }
                                    val actual = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maximumVolume
                                    currentFeedback(AndroidPlayerFeedback(
                                        message = "$volumeLabel ${percent.format(actual)}",
                                        level = actual,
                                        onLeft = true,
                                    ))
                                }
                            } else {
                                activity.window.attributes = activity.window.attributes.apply { screenBrightness = value }
                                currentFeedback(AndroidPlayerFeedback(
                                    message = "$brightnessLabel ${percent.format(value)}",
                                    level = value,
                                ))
                            }
                        }
                    },
                )
            },
    )
}
