package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.components.ShelfHeader
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.home.model.HomeSectionUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_continue_watching
import embyex.shared.generated.resources.home_section_downloaded
import embyex.shared.generated.resources.home_section_favorites
import embyex.shared.generated.resources.home_section_new_in
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeShelf(
    section: HomeSectionUi,
    layout: HomeLayout,
    menuCardId: String?,
    onOpenCard: (HomeCardUi) -> Unit,
    onOpenMenu: (HomeCardUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f,
    onSeeAll: (() -> Unit)? = null,
    menu: @Composable (HomeCardUi) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val title = section.kind.title()
    val wide = section.kind == HomeSectionKind.ContinueWatching || section.kind == HomeSectionKind.Downloaded
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        ShelfHeader(
            title = title,
            listState = listState,
            showScrollButtons = layout.isSideBySide && !section.failed,
            horizontalPadding = layout.gutter,
            count = section.total?.toString(),
            onSeeAll = onSeeAll.takeUnless { section.failed },
        )
        if (section.failed) {
            HomeSectionError(
                title = title,
                onRetry = onRetry,
                modifier = Modifier.padding(horizontal = layout.gutter),
            )
            return@Column
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                horizontal = layout.gutter,
                vertical = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = if (layout == HomeLayout.Phone) 12.dp else 16.dp),
        ) {
            items(
                items = section.cards,
                key = { it.id },
            ) { card ->
                if (wide) {
                    HomeWideCard(
                        card = card,
                        width = layout.wideWidth * cardScale,
                        onOpen = { onOpenCard(card) },
                        onOpenMenu = { onOpenMenu(card) },
                        menu = { if (menuCardId == card.id) menu(card) },
                    )
                } else {
                    HomePosterCard(
                        card = card,
                        width = layout.posterWidth * cardScale,
                        onOpen = { onOpenCard(card) },
                        onOpenMenu = { onOpenMenu(card) },
                        menu = { if (menuCardId == card.id) menu(card) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeSectionKind.title(): String = when (this) {
    HomeSectionKind.ContinueWatching -> stringResource(resource = Res.string.home_continue_watching)
    is HomeSectionKind.LatestIn -> stringResource(resource = Res.string.home_section_new_in,libraryName)
    HomeSectionKind.Favorites -> stringResource(resource = Res.string.home_section_favorites)
    HomeSectionKind.Downloaded -> stringResource(resource = Res.string.home_section_downloaded)
}
