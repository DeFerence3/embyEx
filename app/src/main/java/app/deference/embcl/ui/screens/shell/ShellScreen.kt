package app.deference.embcl.ui.screens.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.deference.embcl.core.session.Session
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.LocalNavigator
import app.deference.embcl.ui.screens.home.HomeContent
import app.deference.embcl.ui.screens.home.HomeViewModel
import app.deference.embcl.ui.screens.libraries.LibrariesContent
import app.deference.embcl.ui.screens.libraries.LibrariesViewModel
import app.deference.embcl.ui.screens.library.EmbyLibraryScreen
import app.deference.embcl.ui.screens.openItem
import app.deference.embcl.ui.screens.search.SearchContent
import app.deference.embcl.ui.screens.search.SearchViewModel
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbyShellContent(
	state: ShellState,
	onAction: (ShellAction) -> Unit,
	homeState: app.deference.embcl.ui.screens.home.HomeState,
	onHomeAction: (app.deference.embcl.ui.screens.home.HomeAction) -> Unit,
	librariesState: app.deference.embcl.ui.screens.libraries.LibrariesState,
	onLibrariesAction: (app.deference.embcl.ui.screens.libraries.LibrariesAction) -> Unit,
	searchState: app.deference.embcl.ui.screens.search.SearchState,
	onSearchAction: (app.deference.embcl.ui.screens.search.SearchAction) -> Unit,
) {
	val repository = koinInject<EmbyRepository>()
	val backStack = LocalNavigator.current
	val tab = state.selectedTab
	
	Scaffold(
		contentWindowInsets = WindowInsets.navigationBars,
		topBar = {
			TopAppBar(
				title = {
					Column {
						Text(if (tab == EmbyTab.Home) Session.serverName else tab.name)
						if (tab == EmbyTab.Home) {
							Text(
								Session.userName,
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				},
				actions = {
					Box {
						IconButton(onClick = { onAction(ShellAction.SetAccountMenuOpen(true)) }) {
							AsyncImage(
								model = repository.userImageUrl(),
								contentDescription = "Account",
								modifier = Modifier
									.size(36.dp)
									.clip(CircleShape)
									.background(MaterialTheme.colorScheme.secondaryContainer),
								contentScale = ContentScale.Crop,
							)
						}
						DropdownMenu(expanded = state.isAccountMenuOpen, onDismissRequest = { onAction(ShellAction.SetAccountMenuOpen(false)) }) {
							DropdownMenuItem(
								text = { Text("Sign out") },
								leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
								onClick = {
									onAction(ShellAction.SignOut)
								},
							)
						}
					}
				},
				colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
			)
		},
		bottomBar = {
			NavigationBar {
				NavigationBarItem(
					selected = tab == EmbyTab.Home,
					onClick = { onAction(ShellAction.SelectTab(EmbyTab.Home)) },
					icon = { Icon(Icons.Filled.Home, null) },
					label = { Text("Home") },
				)
				NavigationBarItem(
					selected = tab == EmbyTab.Libraries,
					onClick = { onAction(ShellAction.SelectTab(EmbyTab.Libraries)) },
					icon = { Icon(Icons.Filled.VideoLibrary, null) },
					label = { Text("Libraries") },
				)
				NavigationBarItem(
					selected = tab == EmbyTab.Search,
					onClick = { onAction(ShellAction.SelectTab(EmbyTab.Search)) },
					icon = { Icon(Icons.Filled.Search, null) },
					label = { Text("Search") },
				)
			}
		},
	) { padding ->
		AnimatedContent(targetState = tab, label = "emby_tab") { selected ->
			when (selected) {
				EmbyTab.Home -> HomeContent(
					state = homeState,
					onAction = onHomeAction,
					modifier = Modifier.padding(padding),
					repository = repository,
					onItemClick = { item -> openItem(item, backStack) },
					onLibraryClick = { lib -> backStack.goTo(EmbyLibraryScreen(lib.id, lib.name)) },
				)
				
				EmbyTab.Libraries -> LibrariesContent(
					state = librariesState,
					onAction = onLibrariesAction,
					modifier = Modifier.padding(padding),
					repository = repository,
					onLibraryClick = { lib -> backStack.goTo(EmbyLibraryScreen(lib.id, lib.name)) },
				)
				
				EmbyTab.Search -> SearchContent(
					state = searchState,
					onAction = onSearchAction,
					modifier = Modifier.padding(padding),
					repository = repository,
					onItemClick = { item -> openItem(item, backStack) },
				)
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
		val homeState by homeViewModel.state.collectAsState()
		val librariesState by librariesViewModel.state.collectAsState()
		val searchState by searchViewModel.state.collectAsState()
		EmbyShellContent(
			state = shellState,
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
