package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.screens.details.model.EpisodeLinkUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_next_episode
import embyex.shared.generated.resources.details_previous_episode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsEpisodeNav(
    previous: EpisodeLinkUi?,
    next: EpisodeLinkUi?,
    onOpenEpisode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (previous == null && next == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Box(modifier = Modifier.weight(weight = 1f)) {
            previous?.let { link ->
                EpisodeLink(
                    caption = stringResource(
                        resource = Res.string.details_previous_episode,
                        link.seasonNumber,
                        link.episodeNumber,
                    ),
                    link = link,
                    isNext = false,
                    onClick = { onOpenEpisode(link.id) },
                )
            }
        }
        Box(modifier = Modifier.weight(weight = 1f)) {
            next?.let { link ->
                EpisodeLink(
                    caption = stringResource(
                        resource = Res.string.details_next_episode,
                        link.seasonNumber,
                        link.episodeNumber,
                    ),
                    link = link,
                    isNext = true,
                    onClick = { onOpenEpisode(link.id) },
                )
            }
        }
    }
}

@Composable
private fun EpisodeLink(
    caption: String,
    link: EpisodeLinkUi,
    isNext: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .animateWithHover(interactionSource = interactionSource),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = if (isNext) Alignment.End else Alignment.Start,
            ),
        ) {
            if (!isNext) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
                horizontalAlignment = if (isNext) Alignment.End else Alignment.Start,
            ) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (isNext) TextAlign.End else TextAlign.Start,
                )
            }
            if (isNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = null,
                )
            }
        }
    }
}
