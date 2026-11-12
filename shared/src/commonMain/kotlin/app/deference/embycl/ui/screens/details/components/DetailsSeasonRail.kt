package app.deference.embycl.ui.screens.details.components

import app.deference.embycl.ui.core.components.ShelfHeader
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.screens.details.model.SeasonEpisodeUi
import app.deference.embycl.ui.screens.details.model.SeasonUi
import coil3.compose.AsyncImage
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_more_in_season
import embyex.shared.generated.resources.details_now_viewing
import embyex.shared.generated.resources.details_retry
import embyex.shared.generated.resources.details_season_failed
import embyex.shared.generated.resources.details_watched
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsSeasonRail(
    season: SeasonUi,
    horizontalPadding: Dp,
    showScrollButtons: Boolean,
    onOpenEpisode: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (season) {
        SeasonUi.Hidden -> Unit
        SeasonUi.Loading -> Row(
            modifier = modifier.padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            repeat(times = 3) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .width(width = 220.dp)
                        .aspectRatio(ratio = 16f / 9f),
                ) {}
            }
        }
        is SeasonUi.Failed -> Column(
            modifier = modifier.padding(horizontal = horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            SectionTitle(
                text = stringResource(
                    resource = Res.string.details_more_in_season,
                    season.seasonNumber,
                ),
            )
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            resource = Res.string.details_season_failed,
                            season.seasonNumber,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(weight = 1f),
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = stringResource(resource = Res.string.details_retry))
                    }
                }
            }
        }
        is SeasonUi.Loaded -> if (season.episodes.isNotEmpty()) {
            val listState = rememberLazyListState()
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                ShelfHeader(
                    title = stringResource(
                        resource = Res.string.details_more_in_season,
                        season.seasonNumber,
                    ),
                    listState = listState,
                    showScrollButtons = showScrollButtons,
                    horizontalPadding = horizontalPadding,
                )
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    items(
                        items = season.episodes,
                        key = { episode -> episode.id },
                    ) { episode ->
                        EpisodeCard(
                            episode = episode,
                            onClick = { onOpenEpisode(episode.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: SeasonEpisodeUi,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = !episode.isCurrent,
        interactionSource = interactionSource,
        color = colors.background,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .width(width = 220.dp)
            .animateWithHover(interactionSource = interactionSource),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colors.surfaceContainerHigh,
                border = if (episode.isCurrent) {
                    BorderStroke(
                        width = 3.dp,
                        color = colors.primary,
                    )
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = 16f / 9f),
            ) {
                Box {
                    if (!episode.stillUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = episode.stillUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    val badge = when {
                        episode.isCurrent -> stringResource(resource = Res.string.details_now_viewing)
                        episode.isPlayed -> stringResource(resource = Res.string.details_watched)
                        else -> null
                    }
                    badge?.let { text ->
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = colors.surfaceContainer,
                            modifier = Modifier
                                .align(alignment = Alignment.TopEnd)
                                .padding(all = 8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 2.dp,
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(space = 2.dp),
                            ) {
                                if (episode.isPlayed && !episode.isCurrent) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(size = 16.dp),
                                    )
                                }
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                    episode.progress?.let { progress ->
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .align(alignment = Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(all = 8.dp),
                        )
                    }
                }
            }
            Text(
                text = listOfNotNull(
                    episode.episodeNumber?.let { "E$it" },
                    episode.title,
                ).joinToString(separator = " · "),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            episode.runtime?.let { runtime ->
                Text(
                    text = runtime,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
