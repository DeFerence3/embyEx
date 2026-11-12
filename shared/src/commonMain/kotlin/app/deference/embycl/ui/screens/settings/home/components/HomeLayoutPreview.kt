package app.deference.embycl.ui.screens.settings.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.ui.screens.home.components.title
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.settings.home.model.HomeLayoutRowUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_continue_watching
import embyex.shared.generated.resources.home_edit_card_comfortable
import embyex.shared.generated.resources.home_edit_card_large
import embyex.shared.generated.resources.home_preview
import embyex.shared.generated.resources.home_preview_cards
import embyex.shared.generated.resources.home_preview_description
import embyex.shared.generated.resources.home_preview_empty
import embyex.shared.generated.resources.home_preview_more
import embyex.shared.generated.resources.home_preview_sections
import embyex.shared.generated.resources.home_preview_top
import org.jetbrains.compose.resources.stringResource

/** Rows in the drawing, the spotlight included; the drawing always has room for all of them. */
private const val SLOTS = 4
private const val CARDS_PER_SHELF = 14

/** Same factor Home applies to shelf cards for [HomeCardSize.Large]. */
private const val LARGE_CARD_SCALE = 1.35f
private val PosterWidth = 20.dp
private val PosterHeight = 30.dp
private val WideWidth = 44.dp
private val WideHeight = 25.dp

/** Tallest card a slot can hold, so a slot is the same height for every card size. */
private val SlotCardHeight = PosterHeight * LARGE_CARD_SCALE

/**
 * A schematic Home that follows the editor as it changes: the Continue watching spotlight when it's at the top, then
 * the shown sections in order with cards at the chosen size. It keeps one height whatever is chosen, so the controls
 * below never move under the pointer. Screen readers get one summary instead of the drawing.
 */
@Composable
internal fun HomeLayoutPreview(
    top: HomeTop,
    cardSize: HomeCardSize,
    rows: List<HomeLayoutRowUi>,
    modifier: Modifier = Modifier,
) {
    val topOn = top == HomeTop.ContinueWatching
    // While Continue watching is the top of Home it isn't also a shelf, as on Home itself.
    val shelves = rows.filter { it.visible && !(topOn && it.kind == HomeSectionKind.ContinueWatching) }
    val titles = shelves.map { it.kind.title() }
    val shelfSlots = if (topOn) SLOTS - 1 else SLOTS
    val drawn = shelves.take(n = shelfSlots)
    val scale = if (cardSize == HomeCardSize.Large) LARGE_CARD_SCALE else 1f
    val description = previewDescription(
        topOn = topOn,
        cardSize = cardSize,
        titles = titles,
    )

    Surface(
        shape = RoundedCornerShape(size = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(
            modifier = Modifier.padding(all = 14.dp),
            verticalArrangement = Arrangement.spacedBy(space = 10.dp),
        ) {
            Text(
                text = stringResource(resource = Res.string.home_preview),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(contentAlignment = Alignment.Center) {
                Column(verticalArrangement = Arrangement.spacedBy(space = 10.dp)) {
                    if (topOn) PreviewSpotlight()
                    drawn.forEachIndexed { index, row ->
                        PreviewShelf(
                            title = titles[index],
                            wide = row.kind == HomeSectionKind.ContinueWatching,
                            scale = scale,
                        )
                    }
                    repeat(times = shelfSlots - drawn.size) { PreviewEmptySlot() }
                }
                if (!topOn && shelves.isEmpty()) {
                    PreviewNote(text = stringResource(resource = Res.string.home_preview_empty))
                }
            }
            // Always laid out, empty when nothing is left over, to keep the height.
            PreviewNote(
                text = if (shelves.size > drawn.size) {
                    stringResource(
                        resource = Res.string.home_preview_more,
                        shelves.size - drawn.size,
                    )
                } else {
                    ""
                },
            )
        }
    }
}

@Composable
private fun previewDescription(
    topOn: Boolean,
    cardSize: HomeCardSize,
    titles: List<String>,
): String {
    val sizeName = when (cardSize) {
        HomeCardSize.Comfortable -> stringResource(resource = Res.string.home_edit_card_comfortable)
        HomeCardSize.Large -> stringResource(resource = Res.string.home_edit_card_large)
    }
    return listOfNotNull(
        stringResource(resource = Res.string.home_preview_description),
        if (topOn) {
            stringResource(
                resource = Res.string.home_preview_top,
                stringResource(resource = Res.string.home_continue_watching),
            )
        } else {
            null
        },
        if (titles.isEmpty()) {
            stringResource(resource = Res.string.home_preview_empty)
        } else {
            stringResource(
                resource = Res.string.home_preview_sections,
                titles.joinToString(separator = ", "),
            )
        },
        stringResource(
            resource = Res.string.home_preview_cards,
            sizeName,
        ),
    ).joinToString(separator = ". ")
}

@Composable
private fun PreviewSlot(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = SlotCardHeight),
        ) { content() }
    }
}

@Composable
private fun PreviewSpotlight() {
    PreviewSlot(title = stringResource(resource = Res.string.home_continue_watching)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(size = 10.dp),
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(size = 22.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(size = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewShelf(
    title: String,
    wide: Boolean,
    scale: Float,
) {
    val cardWidth: Dp = (if (wide) WideWidth else PosterWidth) * scale
    val cardHeight: Dp = (if (wide) WideHeight else PosterHeight) * scale
    PreviewSlot(title = title) {
        // Cards past the edge are clipped, like a shelf that scrolls sideways.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            repeat(times = CARDS_PER_SHELF) {
                Box(
                    modifier = Modifier
                        .width(width = cardWidth)
                        .height(height = cardHeight)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(size = 4.dp),
                        ),
                )
            }
        }
    }
}

/** Keeps the room of an unused row. */
@Composable
private fun PreviewEmptySlot() {
    PreviewSlot(title = "") {}
}

@Composable
private fun PreviewNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
