package app.deference.embycl.ui.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton

@Composable
internal fun AndroidPlayerButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
) {
    PlayerIconButton(
        icon = icon,
        description = description,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        size = size,
    )
}
