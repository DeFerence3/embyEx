package app.deference.embcl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.components.DetailTopBar
import app.deference.embcl.ui.components.EmptyState
import app.deference.embcl.ui.components.LoadState
import app.deference.embcl.ui.components.MediaCard
import app.deference.embcl.ui.core.LocalBackStack
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class EmbyLibraryScreen(val id: String, val title: String) : Screen {
	
	@Composable
	override fun Content() {
		val backStack = LocalBackStack.current
		LibraryContent(
			id = id,
			title = title,
			onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
			onItemClick = { item -> openItem(item, backStack) },
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
	id: String,
	title: String,
	onBack: () -> Unit = {},
	onItemClick: (EmbyItem) -> Unit = {},
) {
	val repository = koinInject<EmbyRepository>()
	val sessionStore = koinInject<EmbySessionStore>()
	val session by sessionStore.session.collectAsState()
	val current = session
	if (current == null) {
		EmptyState("Signed out", "Return to the Emby home screen to sign in.")
		return
	}
	var reload by remember { mutableIntStateOf(0) }
	val state by produceState<Result<EmbyItemsResult>?>(null, current, id, reload) {
		value = runCatching { repository.items(id) }
	}
	Scaffold(
		topBar = {
			DetailTopBar(title) {
				onBack()
			}
		},
	) { padding ->
		LoadState(state, Modifier.padding(padding), onRetry = { reload ++ }) { result ->
			if (result.items.isEmpty()) {
				EmptyState("Nothing here", "This library does not contain any visible media.")
			} else {
				if (result.items.any { it.type == "Episode" }) {
					LazyColumn(
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						verticalArrangement = Arrangement.spacedBy(18.dp),
					) {
						items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
							MediaCard(item, current, repository) { onItemClick(item) }
						}
					}
				}else{
					LazyVerticalGrid(
						columns = GridCells.Adaptive(132.dp),
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						verticalArrangement = Arrangement.spacedBy(18.dp),
					) {
						items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
							MediaCard(item, current, repository) { onItemClick(item) }
						}
					}
				}
			}
		}
	}
}
