package app.deference.embcl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.subtitle
import app.deference.embcl.domain.repository.EmbyRepository
import coil3.compose.AsyncImage

@Composable
fun MediaRow(
	title: String,
	media: List<EmbyItem>,
	session: EmbySession,
	repository: EmbyRepository,
	wide: Boolean = false,
	onItemClick: (EmbyItem) -> Unit,
) {
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Text(
			title,
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.SemiBold,
			modifier = Modifier.padding(horizontal = 16.dp),
		)
		LazyRow(
			contentPadding = PaddingValues(horizontal = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			items(media, key = { it.id }) { item ->
				if (wide) {
					WideMediaCard(item, session, repository) { onItemClick(item) }
				} else {
					Box(Modifier.width(132.dp)) {
						MediaCard(item, session, repository) { onItemClick(item) }
					}
				}
			}
		}
	}
}

@Composable
fun LibraryRow(
	libraries: List<EmbyItem>,
	session: EmbySession,
	repository: EmbyRepository,
	onLibraryClick: (EmbyItem) -> Unit,
) {
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		Text(
			"Your libraries",
			style = MaterialTheme.typography.titleLarge,
			fontWeight = FontWeight.SemiBold,
			modifier = Modifier.padding(horizontal = 16.dp),
		)
		LazyRow(
			contentPadding = PaddingValues(horizontal = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			items(libraries, key = { it.id }) { library ->
				Box(Modifier.width(190.dp)) {
					LibraryCard(library, session, repository) { onLibraryClick(library) }
				}
			}
		}
	}
}

@Composable
fun MediaCard(
	item: EmbyItem,
	session: EmbySession,
	repository: EmbyRepository,
	onClick: () -> Unit,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		verticalArrangement = Arrangement.spacedBy(7.dp),
	) {
		Card(
			shape = RoundedCornerShape(14.dp),
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(2f / 3f),
		) {
			Poster(item, session, repository)
		}
		Text(
			item.name,
			maxLines = 2,
			overflow = TextOverflow.Ellipsis,
			style = MaterialTheme.typography.bodyMedium,
			fontWeight = FontWeight.Medium,
		)
		item.subtitle()?.let {
			Text(
				it,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
fun WideMediaCard(
	item: EmbyItem,
	session: EmbySession,
	repository: EmbyRepository,
	onClick: () -> Unit,
) {
	Card(
		modifier = Modifier
			.width(250.dp)
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(16.dp),
	) {
		Column {
			Box(
				Modifier
					.fillMaxWidth()
					.aspectRatio(16f / 9f),
			) {
				Poster(item, session, repository, backdrop = true)
				item.userData?.playedPercentage?.takeIf { it in 0.1..99.9 }?.let { progress ->
					Box(
						Modifier
							.fillMaxWidth(progress.toFloat() / 100f)
							.height(4.dp)
							.align(Alignment.BottomStart)
							.background(MaterialTheme.colorScheme.primary),
					)
				}
			}
			Column(Modifier.padding(12.dp)) {
				Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
				item.subtitle()?.let {
					Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			}
		}
	}
}

@Composable
fun LibraryCard(
	item: EmbyItem,
	session: EmbySession,
	repository: EmbyRepository,
	onClick: () -> Unit,
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.aspectRatio(16f / 10f)
			.clickable(onClick = onClick),
		shape = RoundedCornerShape(18.dp),
	) {
		Box(Modifier.fillMaxSize()) {
			Poster(item, session, repository, backdrop = true)
			Box(
				Modifier
					.fillMaxSize()
					.background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .78f)))),
			)
			Column(
				Modifier
					.align(Alignment.BottomStart)
					.padding(14.dp),
			) {
				Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
				item.subtitle()?.let {
					Text(it, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
				}
			}
		}
	}
}

@Composable
fun Poster(
	item: EmbyItem,
	session: EmbySession,
	repository: EmbyRepository,
	backdrop: Boolean = false,
) {
	val url = repository.imageUrl(session, item, if (backdrop) "Backdrop" else "Primary")
	Box(
		Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceVariant),
		contentAlignment = Alignment.Center,
	) {
		Icon(
			if (item.isFolder) Icons.Filled.VideoLibrary else Icons.Filled.Movie,
			contentDescription = null,
			modifier = Modifier.size(38.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f),
		)
		if (url != null) {
			AsyncImage(
				model = url,
				contentDescription = item.name,
				modifier = Modifier.fillMaxSize(),
				contentScale = ContentScale.Crop,
			)
		}
	}
}
