package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.shape.CircleShape
import app.deference.embycl.ui.core.focusRing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeSpotlightKind
import app.deference.embycl.ui.screens.home.model.HomeSpotlightUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_kicker_continue
import embyex.shared.generated.resources.home_kicker_continue_single
import embyex.shared.generated.resources.home_kicker_just_added
import embyex.shared.generated.resources.home_more_options
import embyex.shared.generated.resources.home_open_details
import embyex.shared.generated.resources.home_play
import embyex.shared.generated.resources.home_play_label
import embyex.shared.generated.resources.home_resume
import embyex.shared.generated.resources.home_resume_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeSpotlight(
    spotlight: HomeSpotlightUi,
    layout: HomeLayout,
    onPlay: (HomeCardUi) -> Unit,
    onOpenDetails: (HomeCardUi) -> Unit,
    onOpenMenu: (HomeCardUi) -> Unit,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
) {
    val card = spotlight.current
    val background = MaterialTheme.colorScheme.background
    if (layout == HomeLayout.Phone) {
        Box(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 260.dp),
            ) {
                HomeArtwork(
                    url = card.backdropUrl,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.verticalGradient(0.3f to Color.Transparent, 1f to background)),
                )
            }
            SpotlightInfo(
                spotlight = spotlight,
                layout = layout,
                onPlay = onPlay,
                onOpenDetails = onOpenDetails,
                onOpenMenu = onOpenMenu,
                menu = menu,
                modifier = Modifier.padding(
                    top = 216.dp,
                    start = layout.gutter,
                    end = layout.gutter,
                ),
            )
        }
    } else {
        val minHeight = when (layout) {
            HomeLayout.Short -> 0.dp
            HomeLayout.Medium -> 380.dp
            else -> 400.dp
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
            ) {
                // Artwork on the right 60 %; the text column ends inside its fully opaque page-colour band.
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = 0.6f),
                ) {
                    HomeArtwork(
                        url = card.backdropUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = Brush.horizontalGradient(0f to background, 0.28f to background, 0.62f to Color.Transparent))
                            .background(brush = Brush.verticalGradient(0.6f to Color.Transparent, 1f to background)),
                    )
                }
            }
            SpotlightInfo(
                spotlight = spotlight,
                layout = layout,
                onPlay = onPlay,
                onOpenDetails = onOpenDetails,
                onOpenMenu = onOpenMenu,
                menu = menu,
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .fillMaxWidth(fraction = if (layout == HomeLayout.Wide) 0.46f else 0.55f)
                    .padding(
                        start = layout.gutter,
                        end = layout.gutter,
                        top = if (layout == HomeLayout.Short) 16.dp else 40.dp,
                        bottom = 8.dp,
                    ),
            )
        }
    }
}

@Composable
private fun SpotlightInfo(
    spotlight: HomeSpotlightUi,
    layout: HomeLayout,
    onPlay: (HomeCardUi) -> Unit,
    onOpenDetails: (HomeCardUi) -> Unit,
    onOpenMenu: (HomeCardUi) -> Unit,
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = spotlight.current
    val resume = card.kind == HomeCardKind.Resume
    val playText = stringResource(resource = if (resume) Res.string.home_resume else Res.string.home_play)
    val playLabel = stringResource(
        resource = if (resume) Res.string.home_resume_label else Res.string.home_play_label,
        card.accessibilityLabel(),
    )
    val openLabel = stringResource(
        resource = Res.string.home_open_details,
        card.title,
    )
    val big = layout != HomeLayout.Short
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = if (big) 12.dp else 8.dp),
    ) {
        SpotlightKicker(spotlight = spotlight)
        Text(
            text = card.title,
            style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .focusRing(shape = MaterialTheme.shapes.small)
                .clickable(
                    onClickLabel = openLabel,
                    onClick = { onOpenDetails(card) },
                )
                .semantics { heading() },
        )
        card.kicker()?.let {
            Text(
                text = it,
                style = if (big) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val timeLeft = card.timeLeft()
        val progress = card.progress
        if (progress != null || timeLeft != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(weight = 1f)
                            .height(height = 8.dp),
                    )
                }
                timeLeft?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        val buttonHeight = if (big) 64.dp else 56.dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Button(
                onClick = { onPlay(card) },
                contentPadding = PaddingValues(horizontal = 32.dp),
                modifier = Modifier
                    .then(if (layout == HomeLayout.Phone) Modifier.weight(weight = 1f) else Modifier)
                    .focusRing(shape = CircleShape)
                    .heightIn(min = buttonHeight)
                    .semantics { contentDescription = playLabel },
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = playText,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box {
                FilledTonalIconButton(
                    onClick = { onOpenMenu(card) },
                    modifier = Modifier
                        .size(size = buttonHeight)
                        .focusRing(shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(
                            resource = Res.string.home_more_options,
                            card.title,
                        ),
                    )
                }
                menu()
            }
        }
    }
}

@Composable
private fun SpotlightKicker(spotlight: HomeSpotlightUi) {
    val count = spotlight.items.size
    val position = spotlight.selected + 1
    val text = when (spotlight.kind) {
        HomeSpotlightKind.ContinueWatching -> if (count == 1) {
            stringResource(resource = Res.string.home_kicker_continue_single)
        } else {
            stringResource(
                resource = Res.string.home_kicker_continue,
                position,
                count,
            )
        }
        HomeSpotlightKind.JustAdded -> stringResource(
            resource = Res.string.home_kicker_just_added,
            position,
            count,
        )
    }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Icon(
                imageVector = if (spotlight.kind == HomeSpotlightKind.JustAdded) Icons.Filled.NewReleases else Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(size = 18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
