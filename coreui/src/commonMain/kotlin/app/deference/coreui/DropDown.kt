package app.deference.coreui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun DropDown(
	expanded: Boolean,
	items: List<DropDownItem>,
	modifier: Modifier = Modifier,
	onDismiss: () -> Unit,
) {
	DropDown(
		modifier = modifier,
		expanded = expanded,
		onDismiss = onDismiss
	) {
		items.forEach {
			DropdownItem(
				text = it.text,
				icon =it.icon,
				onClick = it.onClick
			)
		}
	}
}

@Composable
fun DropDown(
	expanded: Boolean,
	modifier: Modifier = Modifier,
	onDismiss: () -> Unit,
	/** Transparent lets the content paint its own panel, for example to leave a see-through area. */
	containerColor: Color = MenuDefaults.containerColor,
	shadowElevation: Dp = MenuDefaults.ShadowElevation,
	content: @Composable ColumnScope.() -> Unit
) {
	DropdownMenu(
		modifier = modifier,
		expanded = expanded,
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = containerColor,
		shadowElevation = shadowElevation,
		content = content
	)
}

@Composable
fun DropdownItem(
	text: String,
	modifier: Modifier = Modifier,
	icon: ImageVector? = null,
	enabled: Boolean = true,
	selected: Boolean? = null,
	onClick: (() -> Unit)?,
) {
	DropdownMenuItem(
		modifier = modifier
			.semantics {
				if (selected != null) {
					this.selected = selected
					role = Role.RadioButton
				}
			},
		text = {
			Text(
				text = text,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
		},
		leadingIcon = icon?.let {
			{
				Icon(
					imageVector = it,
					contentDescription = null,
				)
			}
		},
		trailingIcon = selected?.let {
			{
				RadioButton(
					selected = it,
					onClick = null,
					enabled = enabled,
				)
			}
		},
		enabled = enabled,
		onClick = onClick ?: { },
	)
}
