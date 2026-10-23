package app.deference.embycl.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.theme.PlayerColors
import app.deference.embycl.ui.player.PlayerTooltip
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_unlock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun AndroidPlayerUnlock(
    onUnlock: () -> Unit,
    onDragging: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember { mutableFloatStateOf(value = 0f) }
    val limit = with(LocalDensity.current) { 144.dp.toPx() }
    val label = stringResource(resource = Res.string.player_unlock)
    Box(
        modifier = modifier
            .width(width = 208.dp)
            .height(height = 64.dp)
            .clip(shape = RoundedCornerShape(percent = 50))
            .background(color = PlayerColors.controlsSurface)
            .padding(all = 4.dp)
            .semantics { onClick(label = label) { onUnlock(); true } },
    ) {
        Text(
            text = label,
            color = PlayerColors.content,
            modifier = Modifier
                .align(alignment = Alignment.Center)
                .padding(start = 44.dp),
        )
        PlayerTooltip(
            description = label,
            modifier = Modifier
                .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                .size(size = 56.dp),
        ) { dismiss ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = CircleShape)
                    .background(color = PlayerColors.controlsOutline)
                    .draggable(
                        state = rememberDraggableState { delta -> offset = (offset + delta).coerceIn(0f, limit) },
                        orientation = Orientation.Horizontal,
                        onDragStarted = { dismiss(); onDragging(true) },
                        onDragStopped = {
                            if (offset >= limit * 0.85f) onUnlock()
                            offset = 0f
                            onDragging(false)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, tint = PlayerColors.content)
            }
        }
    }
}
