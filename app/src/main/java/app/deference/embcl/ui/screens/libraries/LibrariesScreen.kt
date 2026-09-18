package app.deference.embcl.ui.screens.libraries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.core.components.LibraryCard
import app.deference.embcl.ui.core.components.LoadState

@Composable
fun LibrariesContent(
	state: LibrariesState,
	onAction: (LibrariesAction) -> Unit,
	modifier: Modifier = Modifier,
	repository: EmbyRepository,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	LoadState(state.content, modifier, onRetry = { onAction(LibrariesAction.Retry) }) { libraries ->
		LazyVerticalGrid(
			columns = GridCells.Adaptive(160.dp),
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(16.dp),
			horizontalArrangement = Arrangement.spacedBy(14.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			items(libraries, key = { it.id }) { library ->
				LibraryCard(library, repository) { onLibraryClick(library) }
			}
		}
	}
}
