package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_back
import embyex.player.generated.resources.player_forward
import embyex.player.generated.resources.player_pause
import embyex.player.generated.resources.player_play
import embyex.player.generated.resources.player_position
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.isLoadingOrBuffering
import org.openani.mediamp.togglePlayWhenReady

@Composable
internal fun PlayerControls(
    player: MediampPlayer,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by player.state.collectAsState()
    val position by player.currentPositionMillis.collectAsState()
    val metadata by player.mediaProperties.collectAsState()
    val duration = metadata?.durationMillis ?: 0L
    var seeking by remember(key1 = player) { mutableStateOf<Float?>(value = null) }
    val seekDescription = stringResource(resource = Res.string.player_position)
    Column(modifier = modifier.padding(all = 12.dp)) {
        Slider(
            value = seeking ?: if (duration > 0L) (position.toDouble() / duration).toFloat().coerceIn(0f, 1f) else 0f,
            onValueChange = { seeking = it },
            onValueChangeFinished = {
                seeking?.let { fraction ->
                    dispatchAction { player.seekTo(positionMillis = (duration * fraction.toDouble()).toLong()) }
                }
                seeking = null
            },
            enabled = enabled && duration > 0L && state.mediaStatus == MediaStatus.Ready,
            modifier = Modifier.semantics { contentDescription = seekDescription },
        )
        FlowRow(
            modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
				modifier = Modifier
					.align(alignment = Alignment.CenterVertically),
				verticalAlignment = Alignment.CenterVertically
			) {
                PlayerIconButton(
                    icon = Icons.Default.Replay10,
                    description = stringResource(resource = Res.string.player_back),
                    onClick = { dispatchAction { player.skip(deltaMillis = -10_000L) } },
                    enabled = enabled,
                )
                PlayerIconButton(
                    icon = if (state.playWhenReady) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = stringResource(resource = if (state.playWhenReady) Res.string.player_pause else Res.string.player_play),
                    onClick = { dispatchAction { player.togglePlayWhenReady() } },
                    enabled = enabled,
                )
                PlayerIconButton(
                    icon = Icons.Default.Forward10,
                    description = stringResource(resource = Res.string.player_forward),
                    onClick = { dispatchAction { player.skip(deltaMillis = 10_000L) } },
                    enabled = enabled,
                )
            }
            Text(
                text = "${playbackTime(milliseconds = position)} / ${playbackTime(milliseconds = duration)}",
                modifier = Modifier.align(alignment = Alignment.CenterVertically),
            )
        }
        if (state.isLoadingOrBuffering) CircularProgressIndicator()
    }
}

private fun playbackTime(
    milliseconds: Long,
): String {
    val seconds = milliseconds.coerceAtLeast(minimumValue = 0L) / 1000L
    return "${seconds / 3600}:${(seconds / 60 % 60).toString().padStart(length = 2, padChar = '0')}:${(seconds % 60).toString().padStart(length = 2, padChar = '0')}"
}
