package app.deference.embycl.ui.core

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
	val pressed by interactionSource.collectIsPressedAsState()
	
	val scale by animateFloatAsState(
		targetValue = when {
			pressed -> 0.97f
			hovered -> 1.025f
			else -> 1f
		},
		animationSpec = tween(120),
		label = label,
	)
	graphicsLayer {
		scaleX = scale
		scaleY = scale
	}
}