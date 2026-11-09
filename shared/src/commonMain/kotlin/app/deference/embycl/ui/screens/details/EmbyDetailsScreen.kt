package app.deference.embycl.ui.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.screens.downloads.rememberDownloadPermissionRequest
import app.deference.embycl.ui.screens.library.EmbyLibraryScreen
import app.deference.embycl.ui.screens.player.EmbyPlayerScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EmbyDetailsScreen(val id: String) : Screen {

	@Composable
	override fun Content() {
		val navigator = LocalNavigator.current
		val viewModel = koinViewModel<EmbyDetailsVM>(parameters = { parametersOf(id) })
		val state by viewModel.state.collectAsStateWithLifecycle()
		val permission = rememberDownloadPermissionRequest()

		LifecycleResumeEffect(key1 = id) {
			viewModel.onAction(action = EmbyDetailsAction.Retry)
			onPauseOrDispose { }
		}
		EmbyDetailsScreenContent(
			state = state,
			onAction = viewModel::onAction,
			onBack = { navigator.goBack() },
			onPlay = dropUnlessResumed { navigator.goTo(destination = EmbyPlayerScreen(id = id)) },
			onPlayFromServer = dropUnlessResumed {
				navigator.goTo(
					destination = EmbyPlayerScreen(
						id = id,
						fromServer = true,
					),
				)
			},
			onConfirmDownload = { source ->
				permission { _ ->
					if (viewModel.state.value.choice?.selected == source.key) viewModel.onAction(action = EmbyDetailsAction.ConfirmDownload)
				}
			},
			onOpenSeries = { seriesId, seriesName ->
				navigator.goTo(
					destination = EmbyLibraryScreen(
						id = seriesId,
						title = seriesName,
					),
				)
			},
			onOpenEpisode = { episodeId -> navigator.goTo(destination = EmbyDetailsScreen(id = episodeId)) },
		)
	}
}
