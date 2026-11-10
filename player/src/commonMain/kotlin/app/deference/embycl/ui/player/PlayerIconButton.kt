package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icon button with a tooltip. [shortcut] is shown in the tooltip next to the label, for example "M".
 * [tonal] draws its own container; inside a dock pill the pill is the container, so it is false there and
 * [selected] marks the button whose menu or panel is open.
 */
@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    shortcut: String? = null,
    tonal: Boolean = true,
    selected: Boolean = false,
) {
    PlayerTooltip(
        description = description,
        shortcut = shortcut,
        modifier = modifier,
    ) { dismiss ->
        val content: @Composable () -> Unit = {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(size = if (size > 48.dp) 36.dp else 22.dp),
            )
        }
        if (tonal || selected) {
            FilledTonalIconButton(
                onClick = { dismiss(); onClick() },
                enabled = enabled,
                modifier = Modifier.size(size = size),
                shapes = IconButtonDefaults.shapes(),
                content = content,
            )
        } else {
            IconButton(
                onClick = { dismiss(); onClick() },
                enabled = enabled,
                modifier = Modifier.size(size = size),
                shapes = IconButtonDefaults.shapes(),
                content = content,
            )
        }
    }
}
