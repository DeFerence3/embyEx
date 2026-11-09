package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.model.EpisodeContextUi
import coil3.compose.AsyncImage
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_community_rating
import embyex.shared.generated.resources.details_open_series
import embyex.shared.generated.resources.details_rated
import embyex.shared.generated.resources.details_season_episode
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsIdentity(
    title: String,
    logoUrl: String?,
    episode: EpisodeContextUi?,
    year: String?,
    runtime: String?,
    officialRating: String?,
    communityRating: String?,
    videoLabel: String?,
    genres: List<String>,
    onOpenSeries: (seriesId: String, seriesName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        episode?.let { context ->
            EpisodeKicker(
                context = context,
                onOpenSeries = onOpenSeries,
            )
        }
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.75f)
                    .heightIn(max = 108.dp)
                    .semantics { heading() },
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        MetadataLine(
            year = year,
            runtime = runtime,
            officialRating = officialRating,
            communityRating = communityRating,
            videoLabel = videoLabel,
        )
        if (genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                genres.forEach { genre ->
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 4.dp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeKicker(
    context: EpisodeContextUi,
    onOpenSeries: (seriesId: String, seriesName: String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val seriesId = context.seriesId
        val seriesName = context.seriesName
        if (seriesId != null && seriesName != null) {
            val description = stringResource(
                resource = Res.string.details_open_series,
                seriesName,
            )
            TextButton(
                onClick = {
                    onOpenSeries(
                        seriesId,
                        seriesName,
                    )
                },
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                Text(text = seriesName)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        }
        val season = context.seasonNumber
        val number = context.episodeNumber
        if (season != null && number != null) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(
                        resource = Res.string.details_season_episode,
                        season,
                        number,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MetadataLine(
    year: String?,
    runtime: String?,
    officialRating: String?,
    communityRating: String?,
    videoLabel: String?,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        listOfNotNull(
            year,
            runtime,
        ).forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        officialRating?.let { rating ->
            val description = stringResource(
                resource = Res.string.details_rated,
                rating,
            )
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                Text(
                    text = rating,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(
                        horizontal = 6.dp,
                        vertical = 1.dp,
                    ),
                )
            }
        }
        communityRating?.let { score ->
            val description = stringResource(
                resource = Res.string.details_community_rating,
                score,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(size = 18.dp),
                )
                Text(
                    text = score,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        videoLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
