package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton
import app.deference.embycl.ui.player.PlayerTooltip
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_back
import embyex.player.generated.resources.player_forward
import embyex.player.generated.resources.player_pause
import embyex.player.generated.resources.player_play
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.EmbeddedPlayer

@Composable
internal fun PlayerTransportControls(
    player: EmbeddedPlayer?,
    paused: Boolean,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = player?.state?.collectAsState()?.value
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 24.dp),
    ) {
        PlayerIconButton(
            icon = Icons.Default.Replay10,
            description = stringResource(resource = Res.string.player_back),
            onClick = { player?.let { dispatchAction { it.skip(deltaMillis = -10_000L) } } },
            enabled = enabled && player != null,
        )
        if (state?.isLoadingOrBuffering == true) {
            CircularProgressIndicator(
                modifier = Modifier.size(size = 80.dp),
                color = PlayerColors.content,
            )
        } else {
            val description = stringResource(
                resource = if (paused) Res.string.player_play else Res.string.player_pause,
            )
            PlayerTooltip(description = description) { dismiss ->
                FilledIconButton(
                    onClick = {
                        dismiss()
                        player?.let { dispatchAction { it.togglePlayWhenReady() } }
                    },
                    modifier = Modifier.size(size = 80.dp),
                    enabled = enabled && player != null,
                    shapes = IconButtonDefaults.shapes(
                        shape = if (paused) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                        pressedShape = MaterialTheme.shapes.medium,
                    ),
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = description,
                        modifier = Modifier.size(size = 40.dp),
                    )
                }
            }
        }
        PlayerIconButton(
            icon = Icons.Default.Forward10,
            description = stringResource(resource = Res.string.player_forward),
            onClick = { player?.let { dispatchAction { it.skip(deltaMillis = 10_000L) } } },
            enabled = enabled && player != null,
        )
    }
}
