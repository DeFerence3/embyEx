package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.embycl.ui.player.PlayerIconButton

/** Dock button that owns one menu. The menu state is hoisted so shortcuts (S, A) can open it too. */
@Composable
internal fun PlayerMenuButton(
    icon: ImageVector,
    description: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shortcut: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        PlayerIconButton(
            icon = icon,
            description = description,
            onClick = { onExpandedChange(!expanded) },
            enabled = enabled,
            shortcut = shortcut,
            tonal = false,
            selected = expanded,
        )
        DropDown(
            expanded = expanded,
            modifier = Modifier.widthIn(min = 256.dp, max = 336.dp),
            onDismiss = { onExpandedChange(false) },
            content = content,
        )
    }
}
