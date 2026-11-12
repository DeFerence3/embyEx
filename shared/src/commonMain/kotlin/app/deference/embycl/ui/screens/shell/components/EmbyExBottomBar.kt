package app.deference.embycl.ui.screens.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropDownItem
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.core.LocalDeviceConfiguration
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.downloads.EmbyDownloadsScreen
import app.deference.embycl.ui.screens.settings.EmbySettingsScreen
import app.deference.embycl.ui.screens.shell.EmbyTab
import coil3.compose.AsyncImage
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_edit
import embyex.shared.generated.resources.nav_account_label
import org.jetbrains.compose.resources.stringResource

internal val BottomBarItemPadding = 6.dp

@Composable
fun EmbyExBottomBar(
	currentTab: EmbyTab,
	onAction: (EmbyTab) -> Unit,
	onEditHome: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val navigator = LocalNavigator.current
	var menuExpanded by rememberSaveable { mutableStateOf(false) }
	val stacked = LocalDeviceConfiguration.current == DeviceConfiguration.MOBILE_PORTRAIT
	val editHomeLabel = stringResource(resource = Res.string.home_edit)
	val accountLabel = stringResource(
		resource = Res.string.nav_account_label,
		Session.user.name,
	)
	Box(
		modifier = modifier
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
						icon = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
						label = destination.label,
						isSelected = isSelected,
						stacked = stacked,
						onClick = { onAction(destination) }
					)
				}
			}
			Box {
				IconButton(
					onClick = { menuExpanded = true },
					modifier = Modifier
						.focusRing(shape = CircleShape)
						.size(size = if (stacked) 64.dp else 56.dp)
						.semantics { contentDescription = accountLabel },
				) {
					Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(if (stacked) 48.dp else 40.dp)) {
						Box(contentAlignment = Alignment.Center) {
							Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
							text = editHomeLabel,
							icon = Icons.Outlined.Tune,
							onClick = {
								menuExpanded = false
								onEditHome()
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
	stacked: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	val density = LocalDensity.current
	CompositionLocalProvider(
		LocalDensity provides Density(
			density = density.density,
			fontScale = density.fontScale.coerceAtMost(1.5f),
		),
	) {
		val content: @Composable () -> Unit = {
			Icon(
				imageVector = icon,
				contentDescription = null,
			)
			Text(
				text = label,
				style = if (stacked) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
				fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
				maxLines = 1,
			)
		}
		val itemModifier = modifier
			.padding(BottomBarItemPadding)
			.focusRing(shape = CircleShape)
			.clip(CircleShape)
			.selectable(
				selected = isSelected,
				role = Role.Tab,
				onClick = onClick,
			)
		if (stacked) {
			Column(
				modifier = itemModifier
					.heightIn(min = 56.dp)
					.widthIn(min = 72.dp)
					.padding(horizontal = 12.dp, vertical = 6.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
			) { content() }
		} else {
			Row(
				modifier = itemModifier
					.heightIn(min = 48.dp)
					.padding(horizontal = 16.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) { content() }
		}
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