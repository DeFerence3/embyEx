package app.deference.embycl.ui.screens.libraries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.components.LibraryCard
import app.deference.embycl.ui.core.components.LoadState

@Composable
fun LibrariesContent(
	state: LibrariesState,
	onAction: (LibrariesAction) -> Unit,
	modifier: Modifier = Modifier,
	bottomSpacing: Dp = 0.dp,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	LoadState(state.content, modifier, onRetry = { onAction(LibrariesAction.Retry) }) { libraries ->
		Column(modifier = Modifier.fillMaxSize()) {
			LazyVerticalGrid(
				columns = GridCells.Adaptive(160.dp),
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(16.dp),
				horizontalArrangement = Arrangement.spacedBy(14.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp),
			) {
				items(libraries, key = { it.id }) { library ->
					LibraryCard(library) { onLibraryClick(library) }
				}
			}
			Spacer(modifier = Modifier.height(bottomSpacing))
		}
	}
}
