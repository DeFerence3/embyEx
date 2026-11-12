package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.core.artworkZoom
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_downloaded
import embyex.shared.generated.resources.home_more_options
import embyex.shared.generated.resources.home_open_details
import embyex.shared.generated.resources.home_watched
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomePosterCard(
    card: HomeCardUi,
    width: Dp,
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val label = card.accessibilityLabel()
    val moreLabel = stringResource(
        resource = Res.string.home_more_options,
        card.title,
    )
    val openLabel = stringResource(
        resource = Res.string.home_open_details,
        card.title,
    )
    // Episodes show their series as the poster's title, the episode as the line under it.
    val primary = card.seriesName ?: card.title
    val secondary = if (card.seriesName != null) listOfNotNull(card.episodeLabel, card.title).joinToString(separator = " · ") else card.year?.toString()
    val textScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    Column(
        modifier = modifier
            .width(width = width * textScale)
            .focusRing(shape = MaterialTheme.shapes.medium)
            // Label and the menu action live on the clickable node itself, so screen readers announce one labelled button.
            .semantics {
                contentDescription = label
                customActions = listOf(
                    CustomAccessibilityAction(label = moreLabel) {
                        onOpenMenu()
                        true
                    },
                )
            }
            .combinedClickable(
                interactionSource = interaction,
                onClick = onOpen,
                onClickLabel = openLabel,
                onLongClick = onOpenMenu,
                onLongClickLabel = moreLabel,
                role = Role.Button,
            )
            .cardMenuTriggers(onOpenMenu = onOpenMenu),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 2f / 3f)
                .clearAndSetSemantics {}
                .animateWithHover(interactionSource = interaction)
                .clip(shape = MaterialTheme.shapes.medium),
        ) {
            HomeArtwork(
                url = card.posterUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .artworkZoom(interactionSource = interaction),
            )
            when {
                card.isWatched -> HomeArtBadge(
                    icon = Icons.Filled.Check,
                    label = stringResource(resource = Res.string.home_watched),
                    modifier = Modifier
                        .align(alignment = Alignment.TopEnd)
                        .padding(all = 6.dp),
                )
                card.isDownloaded -> HomeArtBadge(
                    icon = Icons.Filled.DownloadDone,
                    label = stringResource(resource = Res.string.home_downloaded),
                    modifier = Modifier
                        .align(alignment = Alignment.TopEnd)
                        .padding(all = 6.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .weight(weight = 1f)
                    .clearAndSetSemantics {}
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp),
            ) {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                secondary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.focusRing(shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = moreLabel,
                    )
                }
                menu()
            }
        }
    }
}

@Composable
internal fun HomeArtBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(all = 4.dp)
                .size(size = 16.dp),
        )
    }
}
