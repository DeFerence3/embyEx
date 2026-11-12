package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.core.artworkZoom
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeSpotlightUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_next_title
import embyex.shared.generated.resources.home_next_up
import embyex.shared.generated.resources.home_position
import embyex.shared.generated.resources.home_previous_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeFilmstrip(
    spotlight: HomeSpotlightUi,
    layout: HomeLayout,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (spotlight.items.size < 2) return
    val listState = rememberLazyListState()
    val selected = spotlight.selected
    val last = spotlight.items.lastIndex
    LaunchedEffect(selected) {
        val visible = listState.layoutInfo.visibleItemsInfo.map { it.index }
        if (selected !in visible.drop(1).dropLast(1)) listState.animateScrollToItem(index = selected.coerceAtLeast(0))
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = layout.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = stringResource(
                    resource = Res.string.home_position,
                    selected + 1,
                    spotlight.items.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(weight = 1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (layout.isSideBySide) {
                FilledTonalIconButton(
                    onClick = { onSelect(selected - 1) },
                    enabled = selected > 0,
                    modifier = Modifier.focusRing(shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(resource = Res.string.home_previous_title),
                    )
                }
                FilledTonalIconButton(
                    onClick = { onSelect(selected + 1) },
                    enabled = selected < last,
                    modifier = Modifier.focusRing(shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(resource = Res.string.home_next_title),
                    )
                }
            }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                horizontal = layout.gutter,
                vertical = 6.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            modifier = Modifier
                .selectableGroup()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onSelect((selected - 1).coerceAtLeast(0))
                            true
                        }
                        Key.DirectionRight -> {
                            onSelect((selected + 1).coerceAtMost(last))
                            true
                        }
                        Key.MoveHome -> {
                            onSelect(0)
                            true
                        }
                        Key.MoveEnd -> {
                            onSelect(last)
                            true
                        }
                        else -> false
                    }
                },
        ) {
            itemsIndexed(
                items = spotlight.items,
                key = { _, card -> card.id },
            ) { index, card ->
                FilmstripThumb(
                    card = card,
                    selected = index == selected,
                    layout = layout,
                    onSelect = { onSelect(index) },
                )
            }
        }
    }
}

@Composable
private fun FilmstripThumb(
    card: HomeCardUi,
    selected: Boolean,
    layout: HomeLayout,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val label = card.accessibilityLabel()
    val width = (if (layout == HomeLayout.Phone || layout == HomeLayout.Short) 112.dp else 152.dp) *
        LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    Column(
        modifier = modifier
            .width(width = width)
            .focusRing(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.onSurface,
            )
            .semantics { contentDescription = label }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Tab,
                onClick = onSelect,
            ),
        verticalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 16f / 9f)
                .clearAndSetSemantics {}
                .animateWithHover(interactionSource = interaction)
                .border(
                    width = 3.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(all = 3.dp)
                .clip(shape = MaterialTheme.shapes.extraSmall),
        ) {
            HomeArtwork(
                url = card.stillUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .artworkZoom(interactionSource = interaction),
            )
            if (card.kind == HomeCardKind.NextUp) {
                HomeArtBadge(
                    icon = Icons.Filled.SkipNext,
                    label = stringResource(resource = Res.string.home_next_up),
                    modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .padding(all = 4.dp),
                )
            }
            card.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(height = 4.dp),
                )
            }
        }
        Text(
            text = card.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
