package app.deference.embycl.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import app.deference.embycl.ui.core.components.PageIntro
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.components.EmptyState
import app.deference.embycl.ui.core.components.LibraryRow
import app.deference.embycl.ui.core.components.LoadState
import app.deference.embycl.ui.core.components.MediaRow

@Composable
fun HomeContent(
	state: HomeState,
	onAction: (HomeAction) -> Unit,
	modifier: Modifier = Modifier,
	bottomSpacing: Dp = 0.dp,
	onItemClick: (EmbyItem) -> Unit,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	LoadState(state.content, modifier, onRetry = { onAction(HomeAction.Retry) }) { home ->
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(vertical = 12.dp),
			verticalArrangement = Arrangement.spacedBy(28.dp),
		) {
			item(key = "intro") {
                PageIntro("YOUR COLLECTION, YOUR TIME", "Find your next great watch.",
                    "Pick up where you left off, or discover something new.",
                    Modifier.padding(horizontal = 20.dp), accent = true)
            }
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
					androidx.compose.foundation.layout.Box(Modifier.height(320.dp)) {
                        EmptyState("Your Emby home is empty", "Add media libraries on your Emby server, then refresh.")
                    }
				}
			}
			item {
				Spacer(modifier = Modifier.height(bottomSpacing))
			}
		}
	}
}
