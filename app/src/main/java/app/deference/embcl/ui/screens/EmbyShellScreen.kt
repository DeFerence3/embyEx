package app.deference.embcl.ui.screens

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.core.LocalBackStack
import app.deference.embcl.ui.core.MainScreen
import coil3.compose.AsyncImage
import org.koin.compose.koinInject

enum class EmbyTab { Home, Libraries, Search }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbyShell(session: EmbySession) {
	val repository = koinInject<EmbyRepository>()
	val backStack = LocalBackStack.current
	var tabIndex by rememberSaveable { mutableIntStateOf(0) }
	var menuExpanded by remember { mutableStateOf(false) }
	val tab = EmbyTab.entries[tabIndex]
	
	Scaffold(
		contentWindowInsets = WindowInsets.navigationBars,
		topBar = {
			TopAppBar(
				title = {
					Column {
						Text(if (tab == EmbyTab.Home) session.serverName else tab.name)
						if (tab == EmbyTab.Home) {
							Text(
								session.userName,
								style = MaterialTheme.typography.labelMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				},
				actions = {
					Box {
						IconButton(onClick = { menuExpanded = true }) {
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
						DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
							DropdownMenuItem(
								text = { Text("Local files") },
								leadingIcon = { Icon(Icons.Filled.Folder, null) },
								onClick = {
									menuExpanded = false
									backStack.add(MainScreen)
								},
							)
							HorizontalDivider()
							DropdownMenuItem(
								text = { Text("Sign out") },
								leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
								onClick = {
									menuExpanded = false
									repository.logout()
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
					onClick = { tabIndex = 0 },
					icon = { Icon(Icons.Filled.Home, null) },
					label = { Text("Home") },
				)
				NavigationBarItem(
					selected = tab == EmbyTab.Libraries,
					onClick = { tabIndex = 1 },
					icon = { Icon(Icons.Filled.VideoLibrary, null) },
					label = { Text("Libraries") },
				)
				NavigationBarItem(
					selected = tab == EmbyTab.Search,
					onClick = { tabIndex = 2 },
					icon = { Icon(Icons.Filled.Search, null) },
					label = { Text("Search") },
				)
			}
		},
	) { padding ->
		AnimatedContent(targetState = tab, label = "emby_tab") { selected ->
			when (selected) {
				EmbyTab.Home -> HomeContent(
					session = session,
					modifier = Modifier.padding(padding),
					repository = repository,
					onItemClick = { item -> openItem(item, backStack) },
					onLibraryClick = { lib -> backStack.add(EmbyLibraryScreen(lib.id, lib.name)) },
				)
				
				EmbyTab.Libraries -> LibrariesContent(
					session = session,
					modifier = Modifier.padding(padding),
					repository = repository,
					onLibraryClick = { lib -> backStack.add(EmbyLibraryScreen(lib.id, lib.name)) },
				)
				
				EmbyTab.Search -> SearchContent(
					session = session,
					modifier = Modifier.padding(padding),
					repository = repository,
					onItemClick = { item -> openItem(item, backStack) },
				)
			}
		}
	}
}
