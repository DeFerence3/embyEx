package app.deference.embycl.ui.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.utils.asRuntime
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.animateWithHover
import coil3.compose.AsyncImage

@Composable
fun MediaRow(title: String, media: List<EmbyItem>, wide: Boolean = false, onItemClick: (EmbyItem) -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
		SectionHeading(title, Modifier.padding(horizontal = 20.dp))
		LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
			items(media, key = { it.id }) { item ->
				if (wide || item.isEpisode()) WideMediaCard(item) { onItemClick(item) }
				else Box(Modifier.width(144.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f))) { MediaCard(item) { onItemClick(item) } }
			}
		}
	}
}

@Composable
fun LibraryRow(libraries: List<EmbyItem>, onLibraryClick: (EmbyItem) -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
		SectionHeading(
			title = "Your libraries",
			modifier = Modifier
				.padding(horizontal = 20.dp)
		)
		LazyRow(
			contentPadding = PaddingValues(horizontal = 20.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			items(libraries, key = { it.id }) { library ->
				Box(Modifier.width(208.dp)) { LibraryCard(library) { onLibraryClick(library) } }
			}
		}
	}
}

@Composable
fun MediaCard(item: EmbyItem, modifier: Modifier = Modifier, onClick: () -> Unit) = if (item.isEpisode()) {
	EpisodeListItem(item, onClick, modifier)
} else {
	val interaction = remember { MutableInteractionSource() }
	Column(
		modifier = modifier
			.fillMaxWidth()
			.clip(MaterialTheme.shapes.medium)
			.clickable(onClick = onClick, role = Role.Button, onClickLabel = "Open ${item.name}", interactionSource = interaction)
			.animateWithHover(interaction),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(2f / 3f)
				.clip(MaterialTheme.shapes.medium)
		) {
			Poster(item)
			if (item.userData?.played == true) Surface(
				modifier = Modifier
					.align(Alignment.TopEnd)
					.padding(8.dp),
				color = MaterialTheme.colorScheme.primaryContainer,
				shape = MaterialTheme.shapes.small,
			) { Icon(
				imageVector = Icons.Default.Check,
				contentDescription = "Watched",
				modifier = Modifier.padding(5.dp).size(18.dp)
			) }
		}
		Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
			Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
			item.subtitle()?.let {
				Text(
					it, maxLines = 1, overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
fun EpisodeListItem(item: EmbyItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
	Card(
		onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
	) {
		Row(
			modifier = Modifier.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Box(
				modifier = Modifier
					.width(if (LocalDensity.current.fontScale > 1.3f) 72.dp else 104.dp)
					.aspectRatio(16f / 9f)
					.clip(MaterialTheme.shapes.small)
			) { Poster(item) }
			
			Column(
				modifier = Modifier.weight(1f),
				verticalArrangement = Arrangement.spacedBy(4.dp)
			) {
				Text(
					text = item.name,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.titleSmall
				)
				item.subtitle()?.let { Text(
					text = it,
					maxLines = 2,
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				) }
				item.runTimeTicks?.asRuntime()?.let { Text(
					text = it,
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.primary
				) }
			}
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowForward,
				contentDescription = null,
				modifier = Modifier.size(20.dp),
				tint = MaterialTheme.colorScheme.primary
			)
		}
	}
}

@Composable
fun WideMediaCard(item: EmbyItem, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	Card(
		onClick = onClick,
		interactionSource = interaction,
		modifier = Modifier.width(272.dp).animateWithHover(interaction),
		shape = MaterialTheme.shapes.large,
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
		) { Poster(item, backdrop = true) }
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(6.dp)
		) {
			Text(
				text = item.name,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.titleMedium
			)
			item.subtitle()?.let { Text(
				text = it,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			) }
			item.userData?.playedPercentage?.takeIf { it in 0.1..99.9 }?.let { progress ->
				LinearProgressIndicator(
					progress = { (progress.toFloat() / 100f).coerceIn(0f, 1f) },
					modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
				)
				Text(
					text = "${progress.toInt()}% watched",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.primary
				)
			}
		}
	}
}

@Composable
fun LibraryCard(item: EmbyItem, onClick: () -> Unit) {
	val interaction = remember { MutableInteractionSource() }
	Card(
		onClick = onClick,
		interactionSource = interaction,
		modifier = Modifier.fillMaxWidth().animateWithHover(interaction),
		shape = RoundedCornerShape(topStart = 12.dp, topEnd = 28.dp, bottomEnd = 12.dp, bottomStart = 28.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
		) {
			Poster(
				item = item,
				backdrop = true
			)
		}
		Row(
			modifier = Modifier.fillMaxWidth().padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text(
				text = item.name,
				modifier = Modifier.weight(1f),
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSecondaryContainer
			)
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowForward,
				contentDescription = null,
				modifier = Modifier.size(20.dp)
			)
		}
	}
}

@Composable
fun Poster(item: EmbyItem, modifier: Modifier = Modifier, backdrop: Boolean = false) {
	val url = item.imageUrl(if (backdrop) "Backdrop" else "Primary") ?: item.imageUrl("Primary")
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surfaceContainerHigh),
		contentAlignment = Alignment.Center
	) {
		Icon(
			imageVector = if (item.isFolder) Icons.Default.VideoLibrary else Icons.Default.Movie,
			contentDescription = null,
			modifier = Modifier.size(38.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant
		)
		if (url != null) AsyncImage(
			model = url,
			contentDescription = null,
			modifier = Modifier.fillMaxSize(),
			contentScale = ContentScale.Crop
		)
	}
}
