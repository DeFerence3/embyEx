package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerTooltip
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_pause
import embyex.player.generated.resources.player_play
import embyex.player.generated.resources.player_buffering
import org.jetbrains.compose.resources.stringResource

/**
 * The one filled control. Round while paused, squircle while playing, so the state never depends on colour.
 * While [buffering] a spinner takes its place; the user's intent (playing) is kept.
 */
@Composable
internal fun PlayerPlayButton(
    paused: Boolean,
    buffering: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    if (buffering) {
        val loading = stringResource(resource = Res.string.player_buffering)
        CircularProgressIndicator(
            modifier = modifier
                .size(size = size)
                .semantics { contentDescription = loading },
        )
        return
    }
    val description = stringResource(resource = if (paused) Res.string.player_play else Res.string.player_pause)
    PlayerTooltip(
        description = description,
        shortcut = "Space",
        modifier = modifier,
    ) { dismiss ->
        FilledIconButton(
            onClick = { dismiss(); onClick() },
            enabled = enabled,
            modifier = Modifier.size(size = size),
            shapes = IconButtonDefaults.shapes(
                shape = if (paused) CircleShape else MaterialTheme.shapes.large,
                pressedShape = MaterialTheme.shapes.small,
            ),
        ) {
            Icon(
                imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = description,
                modifier = Modifier.size(size = size / 2),
            )
        }
    }
}
