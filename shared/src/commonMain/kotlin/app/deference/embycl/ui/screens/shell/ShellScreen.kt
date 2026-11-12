package app.deference.embycl.ui.screens.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalDeviceConfiguration
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.screens.downloads.EmbyDownloadsScreen
import app.deference.embycl.ui.screens.downloads.rememberDownloadPermissionRequest
import app.deference.embycl.ui.screens.home.HomeAction
import app.deference.embycl.ui.screens.settings.home.HomeLayoutSettingsScreen
import app.deference.embycl.ui.screens.home.HomeContent
import app.deference.embycl.ui.screens.home.HomeState
import app.deference.embycl.ui.screens.home.HomeViewModel
import app.deference.embycl.ui.screens.home.components.HomeLayout
import app.deference.embycl.ui.screens.home.components.HomeSourceStatus
import app.deference.embycl.ui.screens.libraries.LibrariesAction
import app.deference.embycl.ui.screens.libraries.LibrariesContent
import app.deference.embycl.ui.screens.libraries.LibrariesState
import app.deference.embycl.ui.screens.libraries.LibrariesViewModel
import app.deference.embycl.ui.screens.library.EmbyLibraryScreen
import app.deference.embycl.ui.screens.openItem
import app.deference.embycl.ui.screens.search.SearchAction
import app.deference.embycl.ui.screens.search.SearchContent
import app.deference.embycl.ui.screens.search.SearchState
import app.deference.embycl.ui.screens.search.SearchViewModel
import app.deference.embycl.ui.screens.shell.components.EmbyExBottomBar
import app.deference.embycl.ui.screens.shell.components.TopAppBar
import app.deference.embycl.ui.screens.signin.EmbySignInScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EmbyShellContent(
	state: ShellState,
	onAction: (ShellAction) -> Unit,
	events: Flow<ShellEvent>,
	homeState: HomeState,
	onHomeAction: (HomeAction) -> Unit,
	librariesState: LibrariesState,
	onLibrariesAction: (LibrariesAction) -> Unit,
	searchState: SearchState,
	onSearchAction: (SearchAction) -> Unit,
) {
	val backStack = LocalNavigator.current
	val tab = state.selectedTab
	var sourceStatusOpen by rememberSaveable { mutableStateOf(false) }
	val downloadPermission = rememberDownloadPermissionRequest()
	val density = LocalDensity.current
	var barHeight by remember { mutableStateOf(0.dp) }

	if (sourceStatusOpen) {
		HomeSourceStatus(
			source = homeState.source,
			serverName = homeState.serverName,
			serverAddress = homeState.serverAddress,
			layout = HomeLayout.from(configuration = LocalDeviceConfiguration.current),
			onRefresh = { onHomeAction(HomeAction.Refresh) },
			onOpenDownloads = { backStack.goTo(EmbyDownloadsScreen) },
			onSignIn = { backStack.goTo(EmbySignInScreen) },
			onDismiss = { sourceStatusOpen = false },
		)
	}
	events.ObserveEvent {
		when (it) {
			ShellEvent.SignedOut -> Unit
		}
	}
	
	Scaffold(
		contentWindowInsets = WindowInsets.navigationBars,
		topBar = {
			TopAppBar(
				currentTab = tab,
				homeState = homeState,
				onRefresh = { onHomeAction(HomeAction.Refresh) },
				onSourceClick = { sourceStatusOpen = true },
			)
		},
		bottomBar = {
			EmbyExBottomBar(
				currentTab = tab,
				onAction = { destination ->
					onAction(ShellAction.SelectTab(destination))
				},
				onEditHome = { backStack.goTo(HomeLayoutSettingsScreen) },
				modifier = Modifier
					.onSizeChanged { barHeight = with(density) { it.height.toDp() } },
			)
		}
	) { paddingValues ->
		val layoutDirection = LocalLayoutDirection.current
		val padding = PaddingValues(
			top = paddingValues.calculateTopPadding(),
			bottom = 0.dp,
			start = paddingValues.calculateStartPadding(layoutDirection),
			end = paddingValues.calculateEndPadding(layoutDirection),
		)
		val bottomSpacing = maxOf(barHeight, paddingValues.calculateBottomPadding())
		Box(modifier = Modifier.fillMaxSize()) {
			AnimatedContent(
				targetState = tab,
				modifier = Modifier
					.padding(padding)
					.consumeWindowInsets(padding),
				transitionSpec = {
					val isForward = targetState.ordinal > initialState.ordinal
					if (isForward) {
						(slideInHorizontally(
							animationSpec = spring(
								stiffness = Spring.StiffnessMediumLow,
								dampingRatio = Spring.DampingRatioNoBouncy
							),
							initialOffsetX = { width -> width / 3 }
						) + fadeIn() + scaleIn(initialScale = 0.96f)).togetherWith(
							slideOutHorizontally(
								animationSpec = spring(
									stiffness = Spring.StiffnessMediumLow,
									dampingRatio = Spring.DampingRatioNoBouncy
								),
								targetOffsetX = { width -> -width / 3 }
							) + fadeOut() + scaleOut(targetScale = 0.96f)
						)
					} else {
						(slideInHorizontally(
							animationSpec = spring(
								stiffness = Spring.StiffnessMediumLow,
								dampingRatio = Spring.DampingRatioNoBouncy
							),
							initialOffsetX = { width -> -width / 3 }
						) + fadeIn() + scaleIn(initialScale = 0.96f)).togetherWith(
							slideOutHorizontally(
								animationSpec = spring(
									stiffness = Spring.StiffnessMediumLow,
									dampingRatio = Spring.DampingRatioNoBouncy
								),
								targetOffsetX = { width -> width / 3 }
							) + fadeOut() + scaleOut(targetScale = 0.96f)
						)
					}
				},
				label = "navigation",
			) { selected ->
				when (selected) {
					EmbyTab.Home -> HomeContent(
						state = homeState,
						onAction = onHomeAction,
						onEditHome = { backStack.goTo(HomeLayoutSettingsScreen) },
						bottomSpacing = bottomSpacing,
						onConfirmDownload = { source ->
							downloadPermission { _ ->
								if (homeState.downloadChoice?.selected == source.key) onHomeAction(HomeAction.ConfirmDownload)
							}
						},
					)

					EmbyTab.Libraries -> LibrariesContent(
						state = librariesState,
						onAction = onLibrariesAction,
						bottomSpacing = bottomSpacing,
						onLibraryClick = { backStack.goTo(EmbyLibraryScreen(it.id, it.name)) }
					)

					EmbyTab.Search -> SearchContent(
						state = searchState,
						onAction = onSearchAction,
						bottomSpacing = bottomSpacing,
						onItemClick = { openItem(it, backStack) }
					)
				}
			}
		}
	}
}

@Serializable
data object EmbyShellScreen : Screen {
	
	@Composable
	override fun Content() {
		val shellViewModel = koinViewModel<ShellViewModel>()
		val homeViewModel = koinViewModel<HomeViewModel>()
		val librariesViewModel = koinViewModel<LibrariesViewModel>()
		val searchViewModel = koinViewModel<SearchViewModel>()
		val shellState by shellViewModel.state.collectAsState()
		val shellEvent = shellViewModel.events
		val homeState by homeViewModel.state.collectAsState()
		val librariesState by librariesViewModel.state.collectAsState()
		val searchState by searchViewModel.state.collectAsState()
		EmbyShellContent(
			state = shellState,
			events = shellEvent,
			onAction = shellViewModel::onAction,
			homeState = homeState,
			onHomeAction = homeViewModel::onAction,
			librariesState = librariesState,
			onLibrariesAction = librariesViewModel::onAction,
			searchState = searchState,
			onSearchAction = searchViewModel::onAction,
		)
	}
}
