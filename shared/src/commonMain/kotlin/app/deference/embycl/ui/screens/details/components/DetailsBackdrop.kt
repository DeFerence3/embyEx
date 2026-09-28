package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun DetailsBackdrop(
	backdrop: String?,
	modifier: Modifier = Modifier,
) {
	Box(
		modifier = modifier
			.background(MaterialTheme.colorScheme.surfaceContainer)
	) {
		if (!backdrop.isNullOrBlank()) {
			AsyncImage(
				model = backdrop,
				contentDescription = null,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
			)
		}
	}
}