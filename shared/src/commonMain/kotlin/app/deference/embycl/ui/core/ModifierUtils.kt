package app.deference.embycl.ui.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.animateWithHover(interactionSource: MutableInteractionSource,label: String = "AnimationWithHover") = composed {
	val hovered by interactionSource.collectIsHoveredAsState()
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val scale by animateFloatAsState(
		targetValue = when {
			pressed -> 0.97f
			hovered || focused -> 1.04f
			else -> 1f
		},
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = label,
	)
	graphicsLayer {
		scaleX = scale
		scaleY = scale
	}
}

fun Modifier.artworkZoom(interactionSource: MutableInteractionSource): Modifier = composed {
	val hovered by interactionSource.collectIsHoveredAsState()
	val focused by interactionSource.collectIsFocusedAsState()
	val scale by animateFloatAsState(
		targetValue = if (hovered || focused) 1.06f else 1f,
		animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
		label = "ArtworkZoom",
	)
	graphicsLayer {
		scaleX = scale
		scaleY = scale
	}
}

fun Modifier.focusRing(
	shape: Shape,
	color: Color = Color.Unspecified,
	width: Dp = 3.dp,
): Modifier = composed {
	var focused by remember { mutableStateOf(false) }
	val ringColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
	this
		.onFocusChanged { focused = it.isFocused }
		.drawWithContent {
			drawContent()
			if (focused) {
				drawOutline(
					outline = shape.createOutline(size = size, layoutDirection = layoutDirection, density = this),
					color = ringColor,
					style = Stroke(width = width.toPx()),
				)
			}
		}
}
