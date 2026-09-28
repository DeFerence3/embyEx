package app.deference.embycl.ui.screens.details.components.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.components.DetailsBackdrop
import app.deference.embycl.ui.screens.details.components.DetailsContent

@Composable
fun PortraitDetailsLayout(
	data: ItemDetailsData,
	tablet: Boolean,
	onPlay: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
	) {
		
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(
					if (tablet) 16f / 9f
					else 16f / 10f
				),
		) {
			
			DetailsBackdrop(
				backdrop = data.backdrop,
				modifier = Modifier.fillMaxSize(),
			)
			
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(
							colorStops = arrayOf(
								0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
								0.55f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
								0.82f to MaterialTheme.colorScheme.surface
									.copy(alpha = 0.70f),
								1f to MaterialTheme.colorScheme.surface,
							)
						)
					)
			)
		}
		
		Box(
			modifier = Modifier.fillMaxWidth(),
			contentAlignment = Alignment.TopCenter,
		) {
			
			DetailsContent(
				data = data,
				compact = !tablet,
				fillPlayButton = !tablet,
				modifier = Modifier
					.fillMaxWidth()
					.widthIn(
						max = if (tablet) 760.dp
						else Dp.Infinity
					)
					.padding(
						horizontal = if (tablet) 32.dp else 20.dp,
						vertical = 16.dp,
					)
					.padding(bottom = 48.dp),
				onPlay = onPlay,
			)
		}
	}
}