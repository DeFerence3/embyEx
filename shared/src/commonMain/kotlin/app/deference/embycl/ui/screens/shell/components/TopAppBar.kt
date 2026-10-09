package app.deference.embycl.ui.screens.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.screens.shell.EmbyTab
import coil3.compose.AsyncImage

@Composable
fun TopAppBar(currentTab: EmbyTab, onRefresh: () -> Unit, onLogout: () -> Unit, onSettings: () -> Unit) {
	var accountMenuOpen by rememberSaveable { mutableStateOf(false) }
	TopAppBar(
		title = {
			Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
				Text(
					if (currentTab == EmbyTab.Home) "EmbyEx" else currentTab.label,
					style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis
				)
				if (currentTab == EmbyTab.Home) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
					Text(
						Session.serverName, Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
						style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis
					)
				}
			}
		},
		actions = {
			if (currentTab == EmbyTab.Home) FilledTonalIconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh home") }
			Box {
				IconButton(onClick = { accountMenuOpen = true }) {
					Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
						Box(contentAlignment = Alignment.Center) {
							Icon(Icons.Default.Person, "Account", tint = MaterialTheme.colorScheme.onPrimaryContainer)
							AsyncImage(Session.user.primaryImageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
						}
					}
				}
				DropdownMenu(expanded = accountMenuOpen, onDismissRequest = { accountMenuOpen = false }, shape = MaterialTheme.shapes.large) {
					DropdownMenuItem(
						text = { Text("Settings") }, leadingIcon = { Icon(Icons.Default.Settings, null) },
						onClick = { accountMenuOpen = false; onSettings() })
					DropdownMenuItem(
						text = { Text("Sign out") }, leadingIcon = { Icon(Icons.AutoMirrored.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
						onClick = { accountMenuOpen = false; onLogout() })
				}
			}
		},
		colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
	)
}
