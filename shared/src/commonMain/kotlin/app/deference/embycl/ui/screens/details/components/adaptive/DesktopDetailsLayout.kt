package app.deference.embycl.ui.screens.details.components.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.components.DetailsBackdrop
import app.deference.embycl.ui.screens.details.components.DetailsContent

@Composable
fun DesktopDetailsLayout(
	data: ItemDetailsData,
	onPlay: () -> Unit,
) {
	Box(
		modifier = Modifier
			.fillMaxSize(),
	) {

		DetailsBackdrop(
			backdrop = data.backdrop,
			modifier = Modifier.fillMaxSize(),
		)

		// Left-side readability.
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.horizontalGradient(
						colorStops = arrayOf(
							0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
							0.30f to MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
							0.58f to MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
							0.80f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
						)
					)
				)
		)

		// Bottom cinematic fade.
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(
					Brush.verticalGradient(
						colorStops = arrayOf(
							0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.10f),
							0.55f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
							1f to MaterialTheme.colorScheme.surface
								.copy(alpha = 0.90f),
						)
					)
				)
		)

		Column(
			modifier = Modifier
				.align(Alignment.CenterStart)
				.fillMaxHeight()
				.fillMaxWidth(0.62f)
				.verticalScroll(rememberScrollState())
				.padding(
					start = 40.dp,
					end = 24.dp,
					top = 88.dp,
					bottom = 64.dp,
				),
		) {

			DetailsContent(
				data = data,
                modifier = Modifier.clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f)).padding(28.dp),
				compact = false,
				fillPlayButton = false,
				onPlay = onPlay,
			)
		}
	}
}
