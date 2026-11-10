package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerAudioLevel
import app.deference.embycl.ui.player.PlayerIconButton
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_feedback_muted
import embyex.player.generated.resources.player_feedback_volume
import embyex.player.generated.resources.player_mute
import embyex.player.generated.resources.player_unmute
import embyex.player.generated.resources.player_volume
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Mute button and, unless [compact], a short volume slider in the same language as the timeline. */
@Composable
internal fun PlayerVolume(
    level: PlayerAudioLevel,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val volume by level.volume.collectAsState()
    val muted by level.muted.collectAsState()
    PlayerIconButton(
        icon = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
        description = stringResource(resource = if (muted) Res.string.player_unmute else Res.string.player_mute),
        onClick = { dispatchAction { level.setMuted(muted = !muted) } },
        shortcut = "M",
        tonal = false,
        modifier = modifier,
    )
    if (compact) return
    val interaction = remember { MutableInteractionSource() }
    val label = stringResource(resource = Res.string.player_volume)
    val value = if (muted) {
        stringResource(resource = Res.string.player_feedback_muted)
    } else {
        stringResource(Res.string.player_feedback_volume, (volume * 100).roundToInt())
    }
    val colors = SliderDefaults.colors(
        thumbColor = if (muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
        activeTrackColor = if (muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
    )
    Slider(
        value = volume.coerceIn(minimumValue = 0f, maximumValue = 1f),
        onValueChange = { target -> dispatchAction { level.setVolume(volume = target) } },
        interactionSource = interaction,
        colors = colors,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interaction,
                colors = colors,
                thumbSize = DpSize(width = 4.dp, height = 22.dp),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                thumbTrackGapSize = 4.dp,
                trackInsideCornerSize = 2.dp,
                drawStopIndicator = null,
                modifier = Modifier.width(width = 96.dp),
            )
        },
        modifier = Modifier
            .width(width = 96.dp)
            .semantics {
                contentDescription = label
                stateDescription = value
            },
    )
}
