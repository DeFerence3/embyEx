package app.deference.embcl.ui.screens.library

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.LocalNavigator
import app.deference.embcl.ui.core.components.DetailTopBar
import app.deference.embcl.ui.core.components.EmptyState
import app.deference.embcl.ui.core.components.LoadState
import app.deference.embcl.ui.core.components.MediaCard
import app.deference.embcl.ui.screens.openItem
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EmbyLibraryScreen(val id: String, val title: String) : Screen {
	
	@Composable
	override fun Content() {
		val backStack = LocalNavigator.current
		val viewModel = koinViewModel<LibraryViewModel>(parameters = { parametersOf(id) })
		val state by viewModel.state.collectAsState()
		LibraryContent(
			title = title,
			state = state,
			onAction = viewModel::onAction,
			onBack = { backStack.goBack() },
			onItemClick = { item -> openItem(item, backStack) },
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
	title: String,
	state: LibraryState,
	onAction: (LibraryAction) -> Unit,
	onBack: () -> Unit = {},
	onItemClick: (EmbyItem) -> Unit = {},
) {
	Scaffold(
		topBar = {
			DetailTopBar(title) {
				onBack()
			}
		},
	) { padding ->
		LoadState(state.content, Modifier.padding(padding), onRetry = { onAction(LibraryAction.Retry) }) { result ->
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
							MediaCard(item) { onItemClick(item) }
						}
					}
				} else {
					LazyVerticalGrid(
						columns = GridCells.Adaptive(132.dp),
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(16.dp),
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						verticalArrangement = Arrangement.spacedBy(18.dp),
					) {
						items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
							MediaCard(item) { onItemClick(item) }
						}
					}
				}
			}
		}
	}
}
