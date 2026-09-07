package app.deference.embcl.ui.screens

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.components.EmptyState
import app.deference.embcl.ui.components.ErrorState
import app.deference.embcl.ui.components.MediaCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchContent(
	session: EmbySession,
	modifier: Modifier = Modifier,
	repository: EmbyRepository,
	onItemClick: (EmbyItem) -> Unit,
) {
	val scope = rememberCoroutineScope()
	var query by rememberSaveable { mutableStateOf("") }
	var results by remember { mutableStateOf<List<EmbyItem>>(emptyList()) }
	var loading by remember { mutableStateOf(false) }
	var error by remember { mutableStateOf<String?>(null) }
	var searchJob by remember { mutableStateOf<Job?>(null) }
	var searchRevision by remember { mutableIntStateOf(0) }
	
	LaunchedEffect(query, searchRevision) {
		searchJob?.cancel()
		if (query.isBlank()) {
			results = emptyList()
			loading = false
			return@LaunchedEffect
		}
		searchJob = scope.launch {
			delay(350.milliseconds)
			loading = true
			error = null
			runCatching { repository.search(session, query) }
				.onSuccess { results = it }
				.onFailure { error = it.message }
			loading = false
		}
	}
	
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 16.dp),
	) {
		OutlinedTextField(
			value = query,
			onValueChange = { query = it },
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 12.dp),
			label = { Text("Search your library") },
			leadingIcon = { Icon(Icons.Filled.Search, null) },
			singleLine = true,
		)
		when {
			loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
			
			error != null -> ErrorState(error.orEmpty()) { searchRevision ++ }
			query.isBlank() -> EmptyState("Find something to watch", "Search movies, shows, seasons, and episodes.")
			results.isEmpty() -> EmptyState("No results", "Nothing matched “$query”.")
			else -> LazyVerticalGrid(
				columns = GridCells.Adaptive(128.dp),
				contentPadding = PaddingValues(bottom = 16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				items(results, key = { it.id }) { item ->
					MediaCard(item, session, repository) { onItemClick(item) }
				}
			}
		}
	}
}
