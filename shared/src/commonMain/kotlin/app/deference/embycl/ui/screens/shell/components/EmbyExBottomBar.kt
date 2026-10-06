package app.deference.embycl.ui.screens.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.shell.EmbyTab

internal val BottomBarItemPadding = 6.dp

@Composable
fun EmbyExBottomBar(
	currentTab: EmbyTab,
	onAction: (EmbyTab) -> Unit,
) {
	Box(
		modifier = Modifier
			.fillMaxWidth(),
		contentAlignment = Alignment.Center
	) {
//		PrimaryTabRow()
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