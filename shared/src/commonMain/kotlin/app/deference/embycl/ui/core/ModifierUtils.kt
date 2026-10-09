package app.deference.embycl.ui.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun Modifier.animateWithHover(interactionSource: MutableInteractionSource,label: String = "AnimationWithHover") = composed {
	val hovered by interactionSource.collectIsHoveredAsState()
	val focused by interactionSource.collectIsFocusedAsState()
	val pressed by interactionSource.collectIsPressedAsState()

	val scale by animateFloatAsState(
		targetValue = when {
			pressed -> 0.97f
			hovered || focused -> 1.015f
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
