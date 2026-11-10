package app.deference.embycl.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * Where the one video surface sits. The surface is composed once, at a fixed place in the tree, because moving
 * it between call sites recreates the native view (a visible cut on Android). Layouts mark the picture's spot
 * with [slot]; the surface follows that spot with [follow], so animated layouts move and resize the picture
 * smoothly. Until a spot is known the surface fills its parent.
 */
@Stable
internal class PlayerPictureSlot {
    private var root: LayoutCoordinates? = null
    private var bounds by mutableStateOf<Rect?>(value = null)

    /** On the Box that holds both the surface and the layouts. */
    fun Modifier.root(): Modifier = onPlaced { root = it }

    /** On the placeholder that has the picture's size and position in the current layout. */
    fun Modifier.slot(): Modifier = onPlaced { placeholder ->
        root?.let { parent -> if (parent.isAttached && placeholder.isAttached) bounds = parent.localBoundingBoxOf(placeholder, clipBounds = false) }
    }

    /** On the surface: measure and place it at the placeholder's bounds; read in layout, so no recomposition. */
    fun Modifier.follow(): Modifier = layout { measurable, constraints ->
        val target = bounds
        val width = target?.width?.roundToInt() ?: constraints.maxWidth
        val height = target?.height?.roundToInt() ?: constraints.maxHeight
        val placeable = measurable.measure(constraints = Constraints.fixed(width = width.coerceAtLeast(minimumValue = 0), height = height.coerceAtLeast(minimumValue = 0)))
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeable.place(x = target?.left?.roundToInt() ?: 0, y = target?.top?.roundToInt() ?: 0)
        }
    }
}
