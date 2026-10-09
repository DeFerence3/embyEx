package app.deference.embycl.ui.screens.libraries

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.components.*

@Composable
fun LibrariesContent(
    state: LibrariesState,
    onAction: (LibrariesAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = 0.dp,
    onLibraryClick: (EmbyItem) -> Unit,
) {
    LoadState(state.content, modifier, onRetry = { onAction(LibrariesAction.Retry) }) { libraries ->
        if (libraries.isEmpty()) {
            EmptyState("Your collection starts here", "Add a library on your Emby server, then refresh.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp + bottomSpacing),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "intro", span = { GridItemSpan(maxLineSpan) }) {
                    PageIntro("${libraries.size} LIBRARIES", "A place for every story.", "Explore your movies, shows, and more.")
                }
                items(libraries, key = { it.id }) { library ->
                    LibraryCard(library) { onLibraryClick(library) }
                }
            }
        }
    }
}
