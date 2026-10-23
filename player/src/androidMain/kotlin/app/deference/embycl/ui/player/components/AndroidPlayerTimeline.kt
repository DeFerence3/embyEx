package app.deference.embycl.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_position
import embyex.player.generated.resources.player_remaining
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer

@Composable
internal fun AndroidPlayerTimeline(
    player: MediampPlayer,
    enabled: Boolean,
    onSeeking: (Boolean) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val state by player.state.collectAsState()
    val duration = properties?.durationMillis ?: 0L
    var seeking by remember(key1 = player) { mutableStateOf<Float?>(value = null) }
    var remaining by remember { mutableStateOf(value = false) }
    val displayed = seeking?.let { (it * duration).toLong() } ?: position
    val seekDescription = stringResource(resource = Res.string.player_position)
    val remainingDescription = stringResource(resource = Res.string.player_remaining)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Text(
            text = androidPlayerTime(milliseconds = displayed),
            color = PlayerColors.content,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelMedium,
        )
        Slider(
            value = seeking ?: if (duration > 0L) (position.toDouble() / duration).toFloat().coerceIn(0f, 1f) else 0f,
            onValueChange = {
                seeking = it
                onSeeking(true)
            },
            onValueChangeFinished = {
                seeking?.let { fraction ->
                    dispatchAction { player.seekTo(positionMillis = (fraction.toDouble() * duration).toLong()) }
                }
                seeking = null
                onSeeking(false)
            },
            enabled = enabled && duration > 0 && state.mediaStatus == MediaStatus.Ready,
            colors = SliderDefaults.colors(
                thumbColor = PlayerColors.content,
                activeTrackColor = PlayerColors.content,
                inactiveTrackColor = PlayerColors.controlsOutline,
            ),
            modifier = Modifier
                .weight(weight = 1f)
                .semantics { contentDescription = seekDescription },
        )
        Text(
            text = (if (remaining) "−" else "") + androidPlayerTime(
                milliseconds = if (remaining) duration - displayed else duration,
            ),
            color = PlayerColors.content,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clickable(onClickLabel = remainingDescription) { remaining = !remaining }
                .height(height = 48.dp)
                .widthIn(min = 56.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
        )
    }
}

internal fun androidPlayerTime(
    milliseconds: Long,
): String {
    val seconds = milliseconds.coerceAtLeast(minimumValue = 0L) / 1000L
    val minutes = (seconds / 60 % 60).toString().padStart(length = 2, padChar = '0')
    val remainder = (seconds % 60).toString().padStart(length = 2, padChar = '0')
    return if (seconds >= 3600) "${seconds / 3600}:$minutes:$remainder" else "$minutes:$remainder"
}
