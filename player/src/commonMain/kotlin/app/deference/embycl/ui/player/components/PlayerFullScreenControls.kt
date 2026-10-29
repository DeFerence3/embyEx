package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton
import app.deference.embycl.ui.player.PlayerTrackControls
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_close
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.EmbeddedPlayer

@Composable
internal fun PlayerFullScreenControls(
    title: String,
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        PlayerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = stringResource(resource = Res.string.player_close),
            onClick = onClose,
        )
        Text(
            text = title,
            color = PlayerColors.content,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = 8.dp),
        )
        if (player != null) {
            PlayerTrackControls(
                player = player,
                enabled = enabled,
                onMenuExpandedChange = onMenuExpandedChange,
                dispatchAction = dispatchAction,
            )
            PlayerPlaybackOptions(
                player = player,
                enabled = enabled,
                onMenuExpandedChange = onMenuExpandedChange,
                dispatchAction = dispatchAction,
            )
        }
    }
}
