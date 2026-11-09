package app.deference.embycl.ui.screens.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropDownItem
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.screens.downloads.EmbyDownloadsScreen
import app.deference.embycl.ui.screens.settings.EmbySettingsScreen
import app.deference.embycl.ui.screens.shell.EmbyTab
import coil3.compose.AsyncImage

internal val BottomBarItemPadding = 6.dp

@Composable
fun EmbyExBottomBar(
	currentTab: EmbyTab,
	onAction: (EmbyTab) -> Unit,
) {
	val navigator = LocalNavigator.current
	var menuExpanded by rememberSaveable { mutableStateOf(false) }
	Box(
		modifier = Modifier
			.fillMaxWidth(),
		contentAlignment = Alignment.Center
	) {
		Row(
			horizontalArrangement = Arrangement.Center,
			verticalAlignment = Alignment.CenterVertically
		) {
			val selectedTabIndex = currentTab.ordinal
			EmbyTab(
				modifier = Modifier
					.padding(vertical = 16.dp),
				indicator = {
					val modifier = Modifier
						.tabIndicatorOffset(selectedTabIndex, matchContentSize = true)
					EmbyBottomBarIndicator(modifier)
				},
				shape = CircleShape
			){
				EmbyTab.entries.forEach { destination ->
					val isSelected = destination == currentTab
					EmbyExBottomBarItem(
						modifier = Modifier,
						icon = destination.unselectedIcon,
						label = destination.label,
						isSelected = isSelected,
						onClick = { onAction(destination) }
					)
				}
			}
			Box {
				IconButton(onClick = { menuExpanded = true }) {
					Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
						Box(contentAlignment = Alignment.Center) {
							Icon(Icons.Default.Person, "Account", tint = MaterialTheme.colorScheme.onPrimaryContainer)
							AsyncImage(Session.user.primaryImageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
						}
					}
				}
				DropDown(
					expanded = menuExpanded,
					items = listOf(
						DropDownItem(
							text = "Downloads",
							icon = Icons.Default.Download,
							onClick = {
								menuExpanded = false
								navigator.goTo(EmbyDownloadsScreen)
							}
						),
						DropDownItem(
							text = "Settings",
							icon = Icons.Default.Settings,
							onClick = {
								menuExpanded = false
								navigator.goTo(EmbySettingsScreen)
							}
						)
					),
					onDismiss = { menuExpanded = false }
				)
			}
		}
	}
}

@Composable
fun EmbyExBottomBarItem(
	isSelected: Boolean,
	icon: ImageVector,
	label: String,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	Row(
		modifier = modifier
			.padding(BottomBarItemPadding)
			.clip(CircleShape)
			.clickable(enabled = true, onClick = onClick)
			.padding(horizontal = 16.dp, vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		AnimatedVisibility(isSelected) {
			Icon(
				imageVector = icon,
				contentDescription = label,
				modifier = Modifier
			)
		}
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium
		)
	}
}

@Composable
fun EmbyBottomBarIndicator(modifier: Modifier) {
	Box(
		modifier
			.padding(BottomBarItemPadding)
			.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
	)
}