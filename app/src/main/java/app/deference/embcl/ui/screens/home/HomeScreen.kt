package app.deference.embcl.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.ui.core.components.EmptyState
import app.deference.embcl.ui.core.components.LibraryRow
import app.deference.embcl.ui.core.components.LoadState
import app.deference.embcl.ui.core.components.MediaRow

@Composable
fun HomeContent(
	state: HomeState,
	onAction: (HomeAction) -> Unit,
	modifier: Modifier = Modifier,
	onItemClick: (EmbyItem) -> Unit,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	LoadState(state.content, modifier, onRetry = { onAction(HomeAction.Retry) }) { home ->
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(vertical = 12.dp),
			verticalArrangement = Arrangement.spacedBy(22.dp),
		) {
			if (home.resume.isNotEmpty()) {
				item {
					MediaRow(
						title = "Continue watching",
						media = home.resume,
						wide = true,
						onItemClick = onItemClick,
					)
				}
			}
			if (home.latest.isNotEmpty()) {
				item {
					MediaRow(
						title = "Recently added",
						media = home.latest,
						onItemClick = onItemClick,
					)
				}
			}
			if (home.views.isNotEmpty()) {
				item {
					LibraryRow(
						libraries = home.views,
						onLibraryClick = onLibraryClick,
					)
				}
			}
			if (home.resume.isEmpty() && home.latest.isEmpty() && home.views.isEmpty()) {
				item {
					EmptyState("Your Emby home is empty", "Add media libraries on your Emby server, then refresh.")
				}
			}
		}
	}
}
