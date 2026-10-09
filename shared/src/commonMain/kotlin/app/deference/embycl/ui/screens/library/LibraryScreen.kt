package app.deference.embycl.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.utils.Log
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.DESKTOP
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_PORTRAIT
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_PORTRAIT
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.core.components.EmptyState
import app.deference.embycl.ui.core.components.LoadState
import app.deference.embycl.ui.core.components.MediaCard
import app.deference.embycl.ui.core.components.SectionHeading
import app.deference.embycl.ui.screens.openItem
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
				val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
				val configuration = DeviceConfiguration.fromWindowSizeClass(windowSizeClass)
				val isEpisodes = result.items.any { it.type == "Episode" }
				val sectionHeading = if (isEpisodes) "Episodes" else "Browse collection"
				val columns = if (isEpisodes) {
					Log.i("LibraryScreen"){ "Configs---> $configuration " }
					when(configuration){
						MOBILE_PORTRAIT -> GridCells.Fixed(1)
						MOBILE_LANDSCAPE -> GridCells.Fixed(2)
						TABLET_PORTRAIT -> GridCells.Fixed(1)
						TABLET_LANDSCAPE -> GridCells.Fixed(3)
						DESKTOP -> GridCells.Fixed(3)
					}
				} else GridCells.Adaptive(132.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f))
				LazyVerticalGrid(
					columns = columns,
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(20.dp),
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalArrangement = Arrangement.spacedBy(18.dp),
				) {
					item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading(sectionHeading, detail = "${result.items.size}") }
					items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
						MediaCard(item) { onItemClick(item) }
					}
				}
/*
				if (result.items.any { it.type == "Episode" }) {
					LazyColumn(
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(20.dp),
						verticalArrangement = Arrangement.spacedBy(18.dp),
					) {
						item { SectionHeading("Episodes", detail = "${result.items.size}") }
						items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
							MediaCard(item) { onItemClick(item) }
						}
					}
				} else {
					val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
					val configuration = DeviceConfiguration.fromWindowSizeClass(windowSizeClass)
					val columns = if (result.items.any { it.type == "Episode" }) {
						when(configuration){
							MOBILE_PORTRAIT -> GridCells.Fixed(1)
							MOBILE_LANDSCAPE -> GridCells.Fixed(2)
							TABLET_PORTRAIT -> GridCells.Fixed(1)
							TABLET_LANDSCAPE -> GridCells.Fixed(2)
							DESKTOP -> GridCells.Fixed(3)
						}
					} else GridCells.Adaptive(132.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f))
					LazyVerticalGrid(
						columns = columns,
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(20.dp),
						horizontalArrangement = Arrangement.spacedBy(16.dp),
						verticalArrangement = Arrangement.spacedBy(18.dp),
					) {
						item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading("Browse collection", detail = "${result.items.size}") }
						items(result.items.sortedBy { it.indexNumber }, key = { it.id }) { item ->
							MediaCard(item) { onItemClick(item) }
						}
					}
				}
*/
			}
		}
	}
}
