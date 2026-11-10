package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerQueueEntry
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_cancel
import embyex.player.generated.resources.player_dismiss_up_next
import embyex.player.generated.resources.player_minutes
import embyex.player.generated.resources.player_play_next
import embyex.player.generated.resources.player_play_now
import embyex.player.generated.resources.player_up_next
import embyex.player.generated.resources.player_up_next_in
import embyex.player.generated.resources.player_up_next_label
import org.jetbrains.compose.resources.stringResource

/**
 * Shown in the last seconds of an episode. With [secondsLeft] it counts down to automatic play; without it
 * (autoplay off) nothing starts by itself. Enter plays now and Esc cancels; see `RegisterPlayerShortcuts`.
 */
@Composable
internal fun PlayerUpNextCard(
    entry: PlayerQueueEntry,
    secondsLeft: Int?,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val upNext = stringResource(resource = Res.string.player_up_next)
    Surface(
        modifier = modifier
            .widthIn(max = 380.dp)
            .semantics {
                paneTitle = upNext
                liveRegion = LiveRegionMode.Polite
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                PlayerQueueThumbnail(uri = entry.artworkUri, modifier = Modifier.width(width = 112.dp))
                Column(modifier = Modifier.weight(weight = 1f)) {
                    Text(
                        text = if (secondsLeft != null) stringResource(Res.string.player_up_next_in, secondsLeft, entry.label)
                        else stringResource(Res.string.player_up_next_label, entry.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.durationMs?.let { duration ->
                        Text(
                            text = stringResource(Res.string.player_minutes, (duration / 60_000L).toInt()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.End),
            ) {
                if (secondsLeft != null) CircularProgressIndicator(
                    progress = { secondsLeft / 10f },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(size = 24.dp),
                )
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(resource = if (secondsLeft != null) Res.string.player_cancel else Res.string.player_dismiss_up_next))
                }
                Button(onClick = onPlayNow) {
                    Text(text = stringResource(resource = if (secondsLeft != null) Res.string.player_play_now else Res.string.player_play_next))
                }
            }
        }
    }
}
