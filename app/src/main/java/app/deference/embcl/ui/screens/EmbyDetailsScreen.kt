package app.deference.embcl.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.asRuntime
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.components.DetailTopBar
import app.deference.embcl.ui.components.EmptyState
import app.deference.embcl.ui.components.LoadState
import app.deference.embcl.ui.core.LocalBackStack
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class EmbyDetailsScreen(val id: String) : Screen {
	
	@Composable
	override fun Content() {
		val backStack = LocalBackStack.current
		DetailsContent(
			id = id,
			onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsContent(
	id: String,
	onBack: () -> Unit = {},
) {
	val repository = koinInject<EmbyRepository>()
	val sessionStore = koinInject<EmbySessionStore>()
	val session by sessionStore.session.collectAsState()
	val current = session
	if (current == null) {
		EmptyState("Signed out", "Return to the Emby home screen to sign in.")
		return
	}
	var reload by remember { mutableIntStateOf(0) }
	val state by produceState<Result<EmbyItem>?>(null, current, id, reload) {
		value = runCatching { repository.item(current, id) }
	}
	Scaffold(
		topBar = {
			DetailTopBar("") {
				onBack()
			}
		},
	) { padding ->
		LoadState(state, Modifier.padding(padding), onRetry = { reload ++ }) { item ->
			ItemDetails(item, current, repository)
		}
	}
}

@Composable
fun ItemDetails(item: EmbyItem, session: EmbySession, repository: EmbyRepository) {
	val context = LocalContext.current
	val backdrop = repository.imageUrl(session, item, type = "Backdrop", maxWidth = 1280)
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState()),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.aspectRatio(16f / 9f)
				.background(MaterialTheme.colorScheme.surfaceVariant),
		) {
			if (backdrop != null) {
				AsyncImage(
					model = backdrop,
					contentDescription = null,
					modifier = Modifier.fillMaxSize(),
					contentScale = ContentScale.Crop,
				)
			}
			Box(
				Modifier
					.fillMaxSize()
					.background(
						Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface)),
					),
			)
		}
		Column(
			modifier = Modifier
				.padding(horizontal = 20.dp)
				.padding(bottom = 32.dp),
			verticalArrangement = Arrangement.spacedBy(14.dp),
		) {
			Text(item.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
			val metadata = listOfNotNull(
				item.productionYear?.toString(),
				item.officialRating,
				item.runTimeTicks?.asRuntime(),
				item.communityRating?.let { "★ %.1f".format(it) },
			)
			if (metadata.isNotEmpty()) {
				Text(metadata.joinToString("  ·  "), color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
			Button(
				onClick = {
					val mpvPlayerIntent = Intent(Intent.ACTION_VIEW).apply {
						val videoUri = repository.streamUrl(session, item).toUri()
						setDataAndType(videoUri, "video/*")
						setClassName("app.marlboroadvance.mpvex", "app.marlboroadvance.mpvex.ui.player.PlayerActivity")
						putExtra("launch_source", "emby")
						putExtra("title", item.name)
						putExtra("emby_item_id", item.id)
						putExtra("position", ((item.userData?.playbackPositionTicks ?: 0L) / 10_000L).toInt())
						putExtra(
							"headers",
							arrayOf(
								"User-Agent", "mpvEx",
								"X-Emby-Token", session.accessToken)
						)
					}
					try {
						context.startActivity(mpvPlayerIntent)
					} catch (e: ActivityNotFoundException) {
						e.printStackTrace()
					}
				},
				modifier = Modifier
					.fillMaxWidth()
					.height(52.dp),
			) {
				Icon(Icons.Filled.PlayArrow, null)
				Spacer(Modifier.width(8.dp))
				Text(if ((item.userData?.playbackPositionTicks ?: 0) > 0) "Resume in mpv" else "Play in mpv")
			}
			item.subtitle()?.takeIf { it != item.productionYear?.toString() }?.let {
				Text(it, style = MaterialTheme.typography.titleMedium)
			}
			item.overview?.takeIf { it.isNotBlank() }?.let {
				Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}
	}
}
