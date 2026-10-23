package app.deference.embycl.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerTooltip
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
    PlayerTooltip(
        description = description,
        modifier = modifier,
    ) { dismiss ->
        Surface(
            onClick = { dismiss(); onClick() },
            enabled = enabled,
            modifier = Modifier.size(size = size),
            shape = CircleShape,
            color = PlayerColors.controlsSurface,
            contentColor = PlayerColors.content,
            border = BorderStroke(width = 1.dp, color = PlayerColors.controlsOutline),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(size = if (size > 48.dp) 36.dp else 22.dp),
                    tint = PlayerColors.content.copy(alpha = if (enabled) 1f else 0.38f),
                )
            }
        }
    }
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
        Surface(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(weight = 1f, fill = false),
            shape = RoundedCornerShape(percent = 50),
            color = PlayerColors.controlsSurface,
            contentColor = PlayerColors.content,
            border = BorderStroke(width = 1.dp, color = PlayerColors.controlsOutline),
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
