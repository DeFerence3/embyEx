package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Segment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerAspectRatio
import app.deference.embycl.ui.player.PlayerFeedback
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_crop
import embyex.player.generated.resources.player_feedback_back
import embyex.player.generated.resources.player_feedback_chapter
import embyex.player.generated.resources.player_feedback_forward
import embyex.player.generated.resources.player_feedback_jump
import embyex.player.generated.resources.player_feedback_muted
import embyex.player.generated.resources.player_feedback_no_next
import embyex.player.generated.resources.player_feedback_no_previous
import embyex.player.generated.resources.player_feedback_pause
import embyex.player.generated.resources.player_feedback_play
import embyex.player.generated.resources.player_feedback_speed
import embyex.player.generated.resources.player_feedback_subtitles_off
import embyex.player.generated.resources.player_feedback_subtitles_on
import embyex.player.generated.resources.player_feedback_unavailable
import embyex.player.generated.resources.player_feedback_volume
import embyex.player.generated.resources.player_fit
import embyex.player.generated.resources.player_stretch
import org.jetbrains.compose.resources.stringResource

/** Confirms a shortcut at the top centre of the picture. A polite live region, so it is also announced. */
@Composable
internal fun PlayerFeedbackPill(
    feedback: PlayerFeedback,
    modifier: Modifier = Modifier,
) {
    val (icon, text) = content(feedback = feedback)
    PlayerPill(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
        if (feedback is PlayerFeedback.Volume) LinearProgressIndicator(
            progress = { feedback.percent / 100f },
            modifier = Modifier.width(width = 120.dp),
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun content(
    feedback: PlayerFeedback,
): Pair<ImageVector, String> = when (feedback) {
    PlayerFeedback.Played -> Icons.Default.PlayArrow to stringResource(resource = Res.string.player_feedback_play)
    PlayerFeedback.Paused -> Icons.Default.Pause to stringResource(resource = Res.string.player_feedback_pause)
    is PlayerFeedback.Seeked -> if (feedback.seconds >= 0) {
        Icons.Default.FastForward to stringResource(Res.string.player_feedback_forward, feedback.seconds)
    } else {
        Icons.Default.FastRewind to stringResource(Res.string.player_feedback_back, -feedback.seconds)
    }
    is PlayerFeedback.Volume -> Icons.AutoMirrored.Filled.VolumeUp to stringResource(Res.string.player_feedback_volume, feedback.percent)
    PlayerFeedback.Muted -> Icons.AutoMirrored.Filled.VolumeOff to stringResource(resource = Res.string.player_feedback_muted)
    is PlayerFeedback.Speed -> Icons.Default.Speed to stringResource(Res.string.player_feedback_speed, speedLabel(speed = feedback.speed))
    is PlayerFeedback.SubtitlesOn -> Icons.Default.Subtitles to stringResource(Res.string.player_feedback_subtitles_on, feedback.label)
    PlayerFeedback.SubtitlesOff -> Icons.Default.SubtitlesOff to stringResource(resource = Res.string.player_feedback_subtitles_off)
    is PlayerFeedback.Aspect -> Icons.Default.AspectRatio to stringResource(
        resource = when (feedback.mode) {
            PlayerAspectRatio.FIT -> Res.string.player_fit
            PlayerAspectRatio.CROP -> Res.string.player_crop
            PlayerAspectRatio.STRETCH -> Res.string.player_stretch
        },
    )
    is PlayerFeedback.Chapter -> Icons.AutoMirrored.Filled.Segment to stringResource(Res.string.player_feedback_chapter, feedback.index, feedback.name)
    is PlayerFeedback.Jumped -> Icons.Default.MoveDown to stringResource(Res.string.player_feedback_jump, feedback.percent, playerTime(milliseconds = feedback.positionMillis))
    PlayerFeedback.NoNextEpisode -> Icons.Default.Block to stringResource(resource = Res.string.player_feedback_no_next)
    PlayerFeedback.NoPreviousEpisode -> Icons.Default.Block to stringResource(resource = Res.string.player_feedback_no_previous)
    PlayerFeedback.Unavailable -> Icons.Default.Block to stringResource(resource = Res.string.player_feedback_unavailable)
}

/** "1×", "1.25×": no trailing zeros. */
internal fun speedLabel(
    speed: Float,
): String {
    val hundredths = kotlin.math.round(speed * 100).toInt()
    val whole = hundredths / 100
    val rest = (hundredths % 100).toString().padStart(length = 2, padChar = '0').trimEnd('0')
    return if (rest.isEmpty()) "$whole×" else "$whole.$rest×"
}
