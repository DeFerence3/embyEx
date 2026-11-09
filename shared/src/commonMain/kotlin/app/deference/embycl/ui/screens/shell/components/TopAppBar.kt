package app.deference.embycl.ui.screens.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.coreui.keyboard.KeyShortcut
import app.deference.coreui.keyboard.RegisterShortcuts
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.screens.shell.EmbyTab

@Composable
fun TopAppBar(
	currentTab: EmbyTab,
	onRefresh: () -> Unit
) {
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
			if (currentTab == EmbyTab.Home) {
				FilledTonalIconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh home") }
				RegisterShortcuts(
					listOf(
						KeyShortcut(
							key = Key.R,
							ctrl = true,
							description = "Refreshes",
							onAction = onRefresh
						)
					),
					key = "RefreshAction",
				)
			}
		},
		colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
	)
}
