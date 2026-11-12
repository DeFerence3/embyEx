package app.deference.embycl.ui.screens.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.download.DownloadSource
import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.ui.core.LocalDeviceConfiguration
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.screens.downloads.components.DownloadSourceDialog
import app.deference.embycl.ui.screens.home.components.HomeCardMenuDropDown
import app.deference.embycl.ui.screens.home.components.HomeCardMenuSheet
import app.deference.embycl.ui.screens.home.components.HomeFilmstrip
import app.deference.embycl.ui.screens.home.components.HomeFullStateView
import app.deference.embycl.ui.screens.home.components.HomeLayout
import app.deference.embycl.ui.screens.home.components.HomeShelf
import app.deference.embycl.ui.screens.home.components.HomeSkeleton
import app.deference.embycl.ui.screens.home.components.HomeSpotlight
import app.deference.embycl.ui.screens.home.components.HomeStatusCallout
import app.deference.embycl.ui.screens.home.components.homeMenuEntries
import app.deference.embycl.ui.screens.home.components.title
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeNotice
import app.deference.embycl.ui.screens.home.model.HomeSectionUi
import app.deference.embycl.ui.screens.home.seeall.HomeSectionScreen
import app.deference.embycl.ui.screens.library.EmbyLibraryScreen
import app.deference.embycl.ui.screens.openItem
import app.deference.embycl.ui.screens.player.EmbyPlayerScreen
import app.deference.embycl.ui.screens.signin.EmbySignInScreen
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_notice_action_failed
import embyex.shared.generated.resources.home_notice_download_queued
import embyex.shared.generated.resources.home_notice_favorite_added
import embyex.shared.generated.resources.home_notice_favorite_removed
import embyex.shared.generated.resources.home_notice_marked_unwatched
import embyex.shared.generated.resources.home_notice_marked_watched
import embyex.shared.generated.resources.home_notice_refresh_failed
import embyex.shared.generated.resources.home_notice_removed
import embyex.shared.generated.resources.home_notice_retry
import embyex.shared.generated.resources.home_notice_undo
import org.jetbrains.compose.resources.getString

private const val LARGE_CARD_SCALE = 1.35f

@Composable
fun HomeContent(
	state: HomeState,
	onAction: (HomeAction) -> Unit,
	onConfirmDownload: (DownloadSource) -> Unit,
	onEditHome: () -> Unit,
	modifier: Modifier = Modifier,
	bottomSpacing: Dp = 0.dp,
) {
	val backStack = LocalNavigator.current
	val koasty = LocalKoasty.current
	val onOpenCard = { card: HomeCardUi ->
		backStack.openItem(
			id = card.id,
			name = card.title,
			type = card.type,
			isFolder = card.isFolder
		)
	}
	val onPlay = { card: HomeCardUi-> backStack.goTo(EmbyPlayerScreen(id = card.id)) }
	val onOpenSeries = { seriesId: String, seriesName: String-> backStack.goTo(EmbyLibraryScreen(seriesId, seriesName)) }
	val onSeeAll = { section: HomeSectionUi, title: String ->
		backStack.goTo(
			HomeSectionScreen(
				key = section.key,
				title = title,
			),
		)
	}
	val onSignIn = { backStack.goTo(EmbySignInScreen) }
	val layout = HomeLayout.from(configuration = LocalDeviceConfiguration.current)
	val currentOnAction by rememberUpdatedState(newValue = onAction)
	var menuCardId by rememberSaveable { mutableStateOf<String?>(null) }
	val content = state.content
	val cardScale = if (state.cardSize == HomeCardSize.Large) LARGE_CARD_SCALE else 1f

	LaunchedEffect(key1 = state.notice) {
		val notice = state.notice ?: return@LaunchedEffect
		val (message, action) = notice.text()
		koasty.show(
			title = message,
			message = action?.let { "Click to $it" },
			onClick = when (notice) {
				is HomeNotice.WatchedChanged,
				is HomeNotice.RemovedFromContinue -> {
					{
						koasty.dismiss()
						currentOnAction(HomeAction.Undo)
					}
				}
				HomeNotice.RefreshFailed -> {
					{
						koasty.dismiss()
						currentOnAction(HomeAction.Refresh)
					}
				}
				is HomeNotice.FavoriteChanged,
				is HomeNotice.ActionFailed,
				HomeNotice.DownloadQueued,
				is HomeNotice.Message -> null
			}
		)
	}

	val menuCard = menuCardId?.let { id -> content?.findCard(itemId = id) }
	val entries = menuCard?.let { card ->
		homeMenuEntries(
			card = card,
			canChangeOnServer = state.canChangeOnServer,
			serverName = state.serverName,
			onToggleWatched = { onAction(HomeAction.ToggleWatched(itemId = card.id)) },
			onToggleFavorite = { onAction(HomeAction.ToggleFavorite(itemId = card.id)) },
			onOpenSeries = { onOpenSeries(card.seriesId.orEmpty(), card.seriesName.orEmpty()) },
			onRemoveFromContinue = { onAction(HomeAction.RemoveFromContinue(itemId = card.id)) },
			onDownload = { onAction(HomeAction.RequestDownload(itemId = card.id)) },
		)
	}

	val anchoredMenu: @Composable (HomeCardUi) -> Unit = { card ->
		if (!layout.usesSheets && entries != null && menuCardId == card.id) {
			HomeCardMenuDropDown(
				expanded = true,
				entries = entries,
				onDismiss = { menuCardId = null },
			)
		}
	}

	PullToRefreshBox(
		isRefreshing = state.isRefreshing,
		onRefresh = { onAction(HomeAction.Refresh) },
		modifier = modifier.fillMaxSize(),
	) {
		LazyColumn(modifier = Modifier.fillMaxSize()) {
			when {
				state.isLoading -> item(key = "skeleton") { HomeSkeleton(layout = layout) }
				state.fullState != null -> item(key = "full-state") {
					HomeFullStateView(
						state = state.fullState,
						serverName = state.serverName,
						onTryAgain = { onAction(HomeAction.Refresh) },
						onSignIn = onSignIn,
					)
				}
				content != null -> {
					item(key = "status") {
						HomeStatusCallout(
							source = state.source,
							serverName = state.serverName,
							onTryAgain = { onAction(HomeAction.Refresh) },
							onSignIn = onSignIn,
							modifier = Modifier.padding(
								start = layout.gutter,
								end = layout.gutter,
								bottom = 16.dp,
							),
						)
					}
					content.spotlight?.let { spotlight ->
						item(key = "spotlight") {
							HomeSpotlight(
								spotlight = spotlight,
								layout = layout,
								onPlay = onPlay,
								onOpenDetails = onOpenCard,
								onOpenMenu = { menuCardId = it.id },
								menu = { anchoredMenu(spotlight.current) },
							)
						}
						item(key = "filmstrip") {
							HomeFilmstrip(
								spotlight = spotlight,
								layout = layout,
								onSelect = { onAction(HomeAction.SelectSlide(index = it)) },
								modifier = Modifier.padding(top = 8.dp),
							)
						}
					}
					items(
						items = content.sections,
						key = { it.key },
					) { section ->
						val title = section.kind.title()
						HomeShelf(
							section = section,
							layout = layout,
							menuCardId = menuCardId,
							onOpenCard = onOpenCard,
							onOpenMenu = { menuCardId = it.id },
							onRetry = { onAction(HomeAction.RetrySection(key = section.key)) },
							cardScale = cardScale,
							onSeeAll = { onSeeAll(section, title) },
							menu = anchoredMenu,
							modifier = Modifier.padding(top = 24.dp),
						)
					}
				}
			}
			item(key = "bottom-spacing") { Spacer(modifier = Modifier.height(height = bottomSpacing + 24.dp)) }
		}
	}

	if (layout.usesSheets && menuCard != null && entries != null) {
		HomeCardMenuSheet(
			card = menuCard,
			entries = entries,
			onDismiss = { menuCardId = null },
		)
	}
	state.downloadChoice?.let { choice ->
		DownloadSourceDialog(
			title = choice.title,
			sources = choice.sources,
			selectedKey = choice.selected,
			serverAvailable = state.canChangeOnServer,
			onSelect = { key -> onAction(HomeAction.SelectSource(key = key)) },
			onConfirm = onConfirmDownload,
			onDismiss = { onAction(HomeAction.DismissDownload) },
			isSubmitting = choice.submitting,
		)
	}
}

private suspend fun HomeNotice.text(): Pair<String, String?> = when (this) {
	is HomeNotice.WatchedChanged -> getString(
		resource = if (watched) Res.string.home_notice_marked_watched else Res.string.home_notice_marked_unwatched,
		title,
	) to getString(resource = Res.string.home_notice_undo)
	is HomeNotice.RemovedFromContinue -> getString(
		resource = Res.string.home_notice_removed,
		title,
	) to getString(resource = Res.string.home_notice_undo)
	is HomeNotice.FavoriteChanged -> getString(
		resource = if (added) Res.string.home_notice_favorite_added else Res.string.home_notice_favorite_removed,
		title,
	) to null
	is HomeNotice.ActionFailed -> getString(
		resource = Res.string.home_notice_action_failed,
		title,
	) to null
	HomeNotice.DownloadQueued -> getString(resource = Res.string.home_notice_download_queued) to null
	is HomeNotice.Message -> text to null
	HomeNotice.RefreshFailed -> getString(resource = Res.string.home_notice_refresh_failed) to getString(resource = Res.string.home_notice_retry)
}
