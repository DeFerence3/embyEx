package app.deference.embcl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyHome
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.components.EmptyState
import app.deference.embcl.ui.components.LibraryRow
import app.deference.embcl.ui.components.LoadState
import app.deference.embcl.ui.components.MediaRow

@Composable
fun HomeContent(
	session: EmbySession,
	modifier: Modifier = Modifier,
	repository: EmbyRepository,
	onItemClick: (EmbyItem) -> Unit,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	var reload by remember { mutableIntStateOf(0) }
	val state by produceState<Result<EmbyHome>?>(null, session, reload) {
		value = runCatching { repository.home() }
	}
	LoadState(state, modifier, onRetry = { reload ++ }) { home ->
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
						session = session,
						repository = repository,
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
						session = session,
						repository = repository,
						onItemClick = onItemClick,
					)
				}
			}
			if (home.views.isNotEmpty()) {
				item {
					LibraryRow(
						libraries = home.views,
						session = session,
						repository = repository,
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
