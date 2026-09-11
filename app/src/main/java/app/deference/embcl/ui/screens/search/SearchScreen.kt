package app.deference.embcl.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.core.components.EmptyState
import app.deference.embcl.ui.core.components.ErrorState
import app.deference.embcl.ui.core.components.MediaCard

@Composable
fun SearchContent(
	session: EmbySession,
	state: SearchState,
	onAction: (SearchAction) -> Unit,
	modifier: Modifier = Modifier,
	repository: EmbyRepository,
	onItemClick: (EmbyItem) -> Unit,
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 16.dp),
	) {
		OutlinedTextField(
			value = state.query,
			onValueChange = { onAction(SearchAction.QueryChanged(it)) },
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 12.dp),
			label = { Text("Search your library") },
			leadingIcon = { Icon(Icons.Filled.Search, null) },
			singleLine = true,
		)
		when {
			state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
			
			state.error != null -> ErrorState(state.error) { onAction(SearchAction.Retry) }
			state.query.isBlank() -> EmptyState("Find something to watch", "Search movies, shows, seasons, and episodes.")
			state.results.isEmpty() -> EmptyState("No results", "Nothing matched “${state.query}”.")
			else -> LazyVerticalGrid(
				columns = GridCells.Adaptive(128.dp),
				contentPadding = PaddingValues(bottom = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				items(state.results, key = { it.id }) { item ->
					MediaCard(item, session, repository) { onItemClick(item) }
				}
			}
		}
	}
}
