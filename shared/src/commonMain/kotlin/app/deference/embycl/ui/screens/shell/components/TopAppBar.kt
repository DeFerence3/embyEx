package app.deference.embycl.ui.screens.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import app.deference.coreui.keyboard.KeyShortcut
import app.deference.coreui.keyboard.RegisterShortcuts
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.LocalDeviceConfiguration
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.HomeState
import app.deference.embycl.ui.screens.home.components.HomeSourceChip
import app.deference.embycl.ui.screens.shell.EmbyTab
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_refresh
import embyex.shared.generated.resources.home_refreshing
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopAppBar(
	currentTab: EmbyTab,
	homeState: HomeState,
	onRefresh: () -> Unit,
	onSourceClick: () -> Unit,
) {
	val home = currentTab == EmbyTab.Home
	val short = LocalDeviceConfiguration.current == DeviceConfiguration.MOBILE_LANDSCAPE
	TopAppBar(
		title = {
			val title: @Composable () -> Unit = {
				Text(
					text = if (home) "EmbyEx" else currentTab.label,
					style = MaterialTheme.typography.titleLarge,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
			val chip: @Composable () -> Unit = {
				if (home) {
					HomeSourceChip(
						source = homeState.source,
						serverName = homeState.serverName,
						onClick = onSourceClick,
					)
				}
			}
			if (short) {
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
				) {
					title()
					chip()
				}
			} else {
				Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
					title()
					chip()
				}
			}
		},
		actions = {
			if (home) {
				val busy = homeState.isRefreshing
				val busyLabel = stringResource(resource = Res.string.home_refreshing)
				FilledTonalIconButton(
					onClick = onRefresh,
					enabled = !busy,
					modifier = Modifier
						.focusRing(shape = CircleShape)
						.then(if (busy) Modifier.semantics { contentDescription = busyLabel } else Modifier),
				) {
					if (busy) {
						CircularProgressIndicator(
							strokeWidth = 2.dp,
							modifier = Modifier.size(size = 20.dp),
						)
					} else {
						Icon(
							imageVector = Icons.Default.Refresh,
							contentDescription = stringResource(resource = Res.string.home_refresh),
						)
					}
				}
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
