package app.deference.embycl.ui.player

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PlayerTooltip(
        description = description,
        modifier = modifier,
    ) { dismiss ->
        IconButton(
            onClick = { dismiss(); onClick() },
            enabled = enabled,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
            )
        }
    }
}
