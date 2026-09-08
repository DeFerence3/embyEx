package app.deference.embcl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.components.LibraryCard
import app.deference.embcl.ui.components.LoadState

@Composable
fun LibrariesContent(
	session: EmbySession,
	modifier: Modifier = Modifier,
	repository: EmbyRepository,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	var reload by remember { mutableIntStateOf(0) }
	val state by produceState<Result<List<EmbyItem>>?>(null, session, reload) {
		value = runCatching { repository.libraries() }
	}
	LoadState(state, modifier, onRetry = { reload ++ }) { libraries ->
		LazyVerticalGrid(
			columns = GridCells.Adaptive(160.dp),
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(16.dp),
			horizontalArrangement = Arrangement.spacedBy(14.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			items(libraries, key = { it.id }) { library ->
				LibraryCard(library, session, repository) { onLibraryClick(library) }
			}
		}
	}
}
