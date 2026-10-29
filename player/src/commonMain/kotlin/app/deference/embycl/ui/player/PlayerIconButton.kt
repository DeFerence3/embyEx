package app.deference.embycl.ui.player

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
) {
    PlayerTooltip(
        description = description,
        modifier = modifier,
    ) { dismiss ->
        FilledTonalIconButton(
            onClick = { dismiss(); onClick() },
            enabled = enabled,
            modifier = Modifier.size(size = size),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(size = if (size > 48.dp) 36.dp else 22.dp),
            )
        }
    }
}
