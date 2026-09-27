package app.deference.embycl.ui.screens.details.components.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.components.DetailsBackdrop
import app.deference.embycl.ui.screens.details.components.DetailsContent

@Composable
fun LandscapeDetailsLayout(
	data: ItemDetailsData,
	tablet: Boolean,
	onPlay: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xFF0B0B0D)),
	) {
		
		Box(
			modifier = Modifier
				.weight(
					if (tablet) 1.15f
					else 0.9f
				)
				.fillMaxHeight(),
		) {
			
			DetailsBackdrop(
				backdrop = data.backdrop,
				modifier = Modifier.fillMaxSize(),
			)
			
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.horizontalGradient(
							colorStops = arrayOf(
								0f to Color.Transparent,
								0.65f to Color.Transparent,
								1f to Color(0xFF0B0B0D),
							)
						)
					)
			)
			
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							listOf(
								Color.Black.copy(alpha = 0.18f),
								Color.Transparent,
								Color.Black.copy(alpha = 0.20f),
							)
						)
					)
			)
		}
		
		Column(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
				.verticalScroll(rememberScrollState())
				.padding(
					start = if (tablet) 28.dp else 18.dp,
					end = if (tablet) 36.dp else 20.dp,
					top = if (tablet) 80.dp else 60.dp,
					bottom = 40.dp,
				),
		) {
			
			DetailsContent(
				data = data,
				
				// Keep landscape phone density compact.
				compact = !tablet,
				
				// Don't make a huge full-width desktop-looking button.
				fillPlayButton = !tablet,
				
				onPlay = onPlay,
			)
		}
	}
}