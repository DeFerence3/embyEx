package app.deference.embycl.ui.screens.shell.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.screens.shell.EmbyTab
import coil3.compose.AsyncImage

@Composable
fun TopAppBar(
	currentTab: EmbyTab,
	onRefresh: () -> Unit,
	onLogout: () -> Unit,
	onSettings: () -> Unit
) {
	var isAccountMenuOpen by rememberSaveable { mutableStateOf(false) }
	TopAppBar(
		title = {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(10.dp)
			) {
				Column {
					Text(
						text = if (currentTab == EmbyTab.Home) Session.serverName else currentTab.name,
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface
					)
					if (currentTab == EmbyTab.Home) {
						Surface(
							color = MaterialTheme.colorScheme.secondaryContainer,
							shape = RoundedCornerShape(8.dp),
							modifier = Modifier.padding(top = 2.dp)
						) {
							Text(
								text = Session.user.name,
								style = MaterialTheme.typography.labelSmall,
								fontWeight = FontWeight.SemiBold,
								color = MaterialTheme.colorScheme.onSecondaryContainer,
								modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
							)
						}
					}
				}
			}
		},
		actions = {
			if (currentTab == EmbyTab.Home) {
				FilledTonalIconButton(
					onClick = onRefresh,//{ onHomeAction(HomeAction.Refresh) },
					shape = CircleShape
				) {
					Icon(
						imageVector = Icons.Filled.Refresh,
						contentDescription = "Refresh"
					)
				}
				Spacer(modifier = Modifier.width(6.dp))
			}
			Box {
				IconButton(
					onClick = { isAccountMenuOpen = true }//{ onAction(ShellAction.SetAccountMenuOpen(true)) }
				) {
					Surface(
						shape = CircleShape,
						color = MaterialTheme.colorScheme.primaryContainer,
						modifier = Modifier
							.size(40.dp)
							.border(
								width = 1.5.dp,
								color = MaterialTheme.colorScheme.outlineVariant,
								shape = CircleShape
							)
					) {
						AsyncImage(
							model = Session.user.primaryImageUrl,
							contentDescription = "Account",
							modifier = Modifier
								.fillMaxSize()
								.clip(CircleShape),
							contentScale = ContentScale.Crop,
							error = rememberVectorPainter(Icons.Default.Person),
							placeholder = rememberVectorPainter(Icons.Default.Person)
						)
					}
				}
				DropdownMenu(
					expanded = isAccountMenuOpen,//state.isAccountMenuOpen,
					onDismissRequest = { isAccountMenuOpen = false },//{ onAction(ShellAction.SetAccountMenuOpen(false)) },
					shape = MaterialTheme.shapes.extraLarge
				) {
					DropdownMenuItem(
						text = {
							Text(
								"Settings",
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = FontWeight.Medium
							)
						},
						leadingIcon = {
							Icon(
								Icons.Filled.Settings,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.primary
							)
						},
						onClick = {
							isAccountMenuOpen = false //onAction(ShellAction.SetAccountMenuOpen(false))
							onSettings() //backStack.goTo(EmbySettingsScreen)
						}
					)
					DropdownMenuItem(
						text = {
							Text(
								"Sign out",
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = FontWeight.Medium
							)
						},
						leadingIcon = {
							Icon(
								Icons.AutoMirrored.Filled.Logout,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.error
							)
						},
						onClick = {
							isAccountMenuOpen = false //onAction(ShellAction.SetAccountMenuOpen(false))
							onLogout() //showSignOutConfirmation = true
						}
					)
				}
			}
		},
		colors = TopAppBarDefaults.topAppBarColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainer
		)
	)
}