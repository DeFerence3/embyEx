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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.core.components.SignOutConfirmationDialog
import app.deference.embycl.ui.screens.home.HomeAction
import app.deference.embycl.ui.screens.home.HomeContent
import app.deference.embycl.ui.screens.home.HomeState
import app.deference.embycl.ui.screens.home.HomeViewModel
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
import app.deference.embycl.ui.screens.settings.EmbySettingsScreen
import app.deference.embycl.ui.screens.shell.components.EmbyExBottomBar
import app.deference.embycl.ui.screens.shell.components.TopAppBar
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
	
	var showSignOutConfirmation by rememberSaveable { mutableStateOf(false) }
	if (showSignOutConfirmation) {
		SignOutConfirmationDialog(
			serverName = Session.serverName,
			onConfirm = {
				showSignOutConfirmation = false
				onAction(ShellAction.SignOut)
			},
			onDismiss = { showSignOutConfirmation = false },
		)
	}
	
	events.ObserveEvent {
		when (it) { ShellEvent.SignedOut -> Unit }
	}
	
	Scaffold(
		contentWindowInsets = WindowInsets.navigationBars,
		topBar = {
			TopAppBar(
				currentTab = tab,
				onRefresh = { onHomeAction(HomeAction.Refresh) },
				onLogout = { showSignOutConfirmation = true },
				onSettings = { backStack.goTo(EmbySettingsScreen) }
			)
		},
		bottomBar = {
			EmbyExBottomBar(
				currentTab = tab,
				onAction = { destination ->
					onAction(ShellAction.SelectTab(destination))
				}
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
		val bottomSpacing = paddingValues.calculateBottomPadding()
		AnimatedContent(
			targetState = tab,
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
			label = "emby_tab_expressive"
		) { selected ->
			when (selected) {
				EmbyTab.Home -> HomeContent(
					state = homeState,
					onAction = onHomeAction,
					modifier = Modifier.padding(padding),
					onItemClick = { item -> openItem(item, backStack) },
					bottomSpacing = bottomSpacing
				) { lib -> backStack.goTo(EmbyLibraryScreen(lib.id, lib.name)) }
				
				EmbyTab.Libraries -> LibrariesContent(
					state = librariesState,
					onAction = onLibrariesAction,
					modifier = Modifier.padding(padding),
					bottomSpacing = bottomSpacing
				) { lib -> backStack.goTo(EmbyLibraryScreen(lib.id, lib.name)) }
				
				EmbyTab.Search -> SearchContent(
					state = searchState,
					onAction = onSearchAction,
					modifier = Modifier.padding(padding),
					bottomSpacing = bottomSpacing
				) { item -> openItem(item, backStack) }
			}
		}
	}
	
	
	/*NavigationSuiteScaffold(
		navigationSuiteItems = {
			EmbyTab.entries.forEach { destination ->
				val isSelected = destination == tab
				item(
					icon = {
						val iconScale by animateFloatAsState(
							targetValue = if (isSelected) 1.15f else 1.0f,
							animationSpec = spring(
								dampingRatio = Spring.DampingRatioMediumBouncy,
								stiffness = Spring.StiffnessMediumLow
							),
							label = "icon_scale_${destination.name}"
						)
						Icon(
							imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
							contentDescription = destination.label,
							modifier = Modifier.scale(iconScale)
						)
					},
					label = {
						Text(
							text = destination.label,
							style = MaterialTheme.typography.labelMedium,
							fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
						)
					},
					selected = isSelected,
					onClick = { onAction(ShellAction.SelectTab(destination)) },
					alwaysShowLabel = false
				)
			}
		}
	) {
	}*/
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
