package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_close
import org.jetbrains.compose.resources.stringResource

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

@Composable
internal fun AndroidPlayerTitle(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AndroidPlayerButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = stringResource(resource = Res.string.player_close),
            onClick = onClose,
        )
        Text(
            text = title,
            modifier = Modifier
                .weight(weight = 1f)
                .padding(start = 16.dp),
            color = PlayerColors.content,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
