package app.deference.embycl.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.components.EmptyState
import app.deference.embycl.ui.core.components.ErrorState
import app.deference.embycl.ui.screens.search.components.SearchResultListView

@Composable
fun SearchContent(
	state: SearchState,
	onAction: (SearchAction) -> Unit,
	modifier: Modifier = Modifier,
	onItemClick: (EmbyItem) -> Unit,
) {
	val searchbarfocus = remember{ FocusRequester() }

	LaunchedEffect(Unit){
		searchbarfocus.requestFocus()
	}
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		OutlinedTextField(
			value = state.query,
			onValueChange = { onAction(SearchAction.QueryChanged(it)) },
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 12.dp)
				.focusRequester(searchbarfocus),
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
			else -> {
				val filters = state.results.map { it.type }.distinct()
				LazyColumn(
					modifier = Modifier
						.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(16.dp)
				) {
					items(filters){ type ->
						SearchResultListView(
							type = type,
							header = type,
							results = state.results.filter { it.type == type }
						){
							onItemClick(it)
						}
					}
				}
			}
		}
	}
}
