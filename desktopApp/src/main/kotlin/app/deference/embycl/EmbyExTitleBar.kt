package app.deference.embycl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.app_name
import embyex.shared.generated.resources.window_close
import embyex.shared.generated.resources.window_maximize
import embyex.shared.generated.resources.window_minimize
import embyex.shared.generated.resources.window_restore
import org.jetbrains.compose.resources.stringResource

private val TITLE_BAR_HEIGHT = 36.dp
private val WINDOW_BUTTON_SIZE = 26.dp
private val WINDOW_BUTTON_ICON_SIZE = 14.dp

/**
 * Title bar for the undecorated main window. The bar has no fill of its own, so it blends
 * into the app background; only the window buttons have a background.
 *
 * Dragging the bar moves the window and double-clicking it toggles maximize.
 * [onCloseRequest] receives the close button, so the app's exit confirmation still applies.
 */
@Composable
internal fun FrameWindowScope.EmbyExTitleBar(
    state: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maximized = state.placement == WindowPlacement.Maximized
    val toggleMaximized: () -> Unit = {
        state.placement = if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
    }
    Box(
        modifier = modifier
            .height(height = TITLE_BAR_HEIGHT)
            .pointerInput(key1 = maximized) {
                detectTapGestures(onDoubleTap = { toggleMaximized() })
            },
    ) {
        WindowDraggableArea(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {

            }
        }
        Row(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .padding(end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WindowButton(
                icon = Icons.Default.Remove,
                contentDescription = stringResource(resource = Res.string.window_minimize),
                onClick = { state.isMinimized = true },
            )
            WindowButton(
                icon = if (maximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                contentDescription = stringResource(
                    resource = if (maximized) Res.string.window_restore else Res.string.window_maximize,
                ),
                onClick = toggleMaximized,
            )
            WindowButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(resource = Res.string.window_close),
                onClick = onCloseRequest,
                destructive = true,
            )
        }
    }
}

/** Mouse-only like native caption buttons: kept out of keyboard focus traversal. */
@Composable
private fun WindowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val container = when {
        hovered && destructive -> colors.error
        hovered -> colors.surfaceContainerHighest
        else -> colors.surfaceContainerHigh
    }
    val content = if (hovered && destructive) colors.onError else colors.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(size = WINDOW_BUTTON_SIZE)
            .clip(shape = CircleShape)
            .background(color = container)
            .focusProperties { canFocus = false }
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = content,
            modifier = Modifier.size(size = WINDOW_BUTTON_ICON_SIZE),
        )
    }
}
