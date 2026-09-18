package app.deference.embcl.ui.screens.details

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.deference.embcl.core.utils.asRuntime
import app.deference.embcl.core.utils.formatToString
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.LocalNavigator
import app.deference.embcl.ui.core.components.LoadingScaffold
import app.deference.embcl.ui.core.components.ObserveEvent
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EmbyDetailsScreen(val id: String) : Screen {
	
	@Composable
	override fun Content() {
		val backStack = LocalNavigator.current
		val viewModel = koinViewModel<EmbyDetailsVM>(parameters = { parametersOf(id) })
		val state by viewModel.state.collectAsState()
		DetailsContent(
			state = state,
			events = viewModel.events,
			onAction = viewModel::onAction,
			onBack = { backStack.goBack() },
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsContent(
	state: EmbyDetailsState,
	events: kotlinx.coroutines.flow.Flow<EmbyDetailsEvent>,
	onAction: (EmbyDetailsAction) -> Unit,
	onBack: () -> Unit = {},
) {
	val mpvLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult(),
	) { result ->
		val data = result.data
		val positionMs = data?.getIntExtra("position", - 1)?.takeIf { it >= 0 }?.toLong()
			?: data?.getLongExtra("position", - 1L)?.takeIf { it >= 0L }
		onAction(EmbyDetailsAction.PlaybackFinished(positionMs))
	}
	events.ObserveEvent { event ->
		when (event) {
			is EmbyDetailsEvent.Error -> Unit
			is EmbyDetailsEvent.LaunchPlayback -> {
				val request = event.request
				val uris = ArrayList(request.urls.map { it.toUri() })
				val intent = Intent(Intent.ACTION_VIEW).apply {
					setDataAndType(uris.getOrNull(request.selectedIndex), "video/*")
					setClassName("app.marlboroadvance.mpvex", "app.marlboroadvance.mpvex.ui.player.PlayerActivity")
					putExtra("launch_source", "emby")
					putExtra("title", request.title)
					putExtra("emby_item_id", request.itemId)
					putExtra("position", request.positionMs)
					putParcelableArrayListExtra("playlist", uris)
					putExtra("playlist_index", request.selectedIndex)
					putExtra("headers", arrayOf("User-Agent", "mpvEx", "X-Emby-Token", request.accessToken))
				}
				try {
					mpvLauncher.launch(intent)
				} catch (_: ActivityNotFoundException) {
				}
			}
		}
	}
	
	LoadingScaffold(
		state = state.content,
		onRetry = { onAction(EmbyDetailsAction.Retry) },
		modifier = Modifier.fillMaxSize(),
		content = { item ->
			ItemDetails(
				item = item,
				onBack = onBack,
				onPlay = { onAction(EmbyDetailsAction.Play) },
			)
		}
	)
}

@Composable
fun ItemDetails(
	item: EmbyItem,
	onBack: () -> Unit,
	onPlay: () -> Unit,
) {
	val backdrop = item.imageUrl(type = "Primary", maxWidth = 1280)
	val logoUrl = item.imageUrl(type = "Logo", maxWidth = 600)
	val videoStream = item.mediaStreams.firstOrNull { it.type == "Video" }
	val subtitleStreams = item.mediaStreams.filter { it.type == "Subtitle" }.joinToString(" | ") { it.displayLanguage ?: it.displayTitle ?: "" }
	val videoResolution = videoStream?.displayTitle
		?: item.container?.uppercase()
		?: "HD"
	val audioTitle = item.mediaStreams.filter { it.type == "Audio" }.joinToString(" | ") { it.displayLanguage ?: it.displayTitle ?: "" }
	val runTimeText = item.runTimeTicks?.asRuntime()
	val currentPositionTicks = item.userData?.playbackPositionTicks ?: 0L
	val totalRunTimeTicks = item.runTimeTicks ?: 0L
	val isResume = currentPositionTicks > 0L
	val progressFraction = if (totalRunTimeTicks > 0L && isResume) {
		(currentPositionTicks.toFloat() / totalRunTimeTicks.toFloat()).coerceIn(0f, 1f)
	} else 0f
	val remainingMinutes = if (totalRunTimeTicks > currentPositionTicks && isResume) {
		val remainingTicks = totalRunTimeTicks - currentPositionTicks
		(remainingTicks / 10_000_000L / 60L).coerceAtLeast(1)
	} else null
	val directors = item.people.filter { it.type == "Director" }.mapNotNull { it.name }
	val writers = item.people.filter { it.type == "Writer" }.mapNotNull { it.name }
	
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xFF101012))
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState()),
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.aspectRatio(16f / 9f),
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
					modifier = Modifier
						.fillMaxSize()
						.background(
							Brush.verticalGradient(
								colorStops = arrayOf(
									0.0f to Color.Black.copy(alpha = 0.5f),
									0.4f to Color.Transparent,
									0.75f to Color(0xFF101012).copy(alpha = 0.7f),
									1.0f to Color(0xFF101012),
								),
							),
						),
				)
			}
			
			Column(
				modifier = Modifier
					.padding(horizontal = 20.dp)
					.padding(top = 4.dp, bottom = 48.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				if (! logoUrl.isNullOrBlank()) {
					AsyncImage(
						model = logoUrl,
						contentDescription = item.seriesName ?: item.name,
						modifier = Modifier
							.height(56.dp)
							.fillMaxWidth(0.6f),
						contentScale = ContentScale.Fit,
						alignment = Alignment.CenterStart,
					)
				}
				
				Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
					val episodeHeader = item.name
					
					Text(
						text = episodeHeader,
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.Bold,
						color = Color.White,
					)
					
					Row(
						horizontalArrangement = Arrangement.spacedBy(12.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						val airDate = item.premiereDate?.formatToString() ?: item.productionYear?.toString()
						if (! airDate.isNullOrBlank()) {
							Text(
								text = airDate,
								style = MaterialTheme.typography.bodyMedium,
								color = Color(0xFFB0B0B8),
							)
						}
						if (! runTimeText.isNullOrBlank()) {
							Text(
								text = runTimeText,
								style = MaterialTheme.typography.bodyMedium,
								color = Color(0xFFB0B0B8),
							)
						}
					}
				}
				
				Row(
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalAlignment = Alignment.Top,
				) {
					Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(
							text = "Video",
							style = MaterialTheme.typography.bodySmall,
							color = Color(0xFF8E8E93),
						)
						Text(
							text = videoResolution,
							style = MaterialTheme.typography.bodySmall,
							fontWeight = FontWeight.Medium,
							color = Color.White,
						)
					}
					
					Row(
						horizontalArrangement = Arrangement.spacedBy(6.dp),
					) {
						Text(
							text = "Audio",
							style = MaterialTheme.typography.bodySmall,
							color = Color(0xFF8E8E93),
						)
						Text(
							text = audioTitle,
							style = MaterialTheme.typography.bodySmall,
							fontWeight = FontWeight.Medium,
							color = Color.White,
						)
					}
				}
				
				if (subtitleStreams.isNotEmpty()) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(10.dp),
					) {
						Text(
							text = "Subtitles",
							style = MaterialTheme.typography.bodySmall,
							color = Color(0xFF8E8E93),
						)
						
						Text(
							text = subtitleStreams.ifBlank { "None" },
							style = MaterialTheme.typography.bodySmall,
							fontWeight = FontWeight.Medium,
							color = Color.White,
						)
					}
				}
				
				Button(
					onClick = onPlay,
					modifier = Modifier
						.fillMaxWidth()
						.height(52.dp),
					shape = RoundedCornerShape(26.dp),
					colors = ButtonDefaults.buttonColors(
						containerColor = Color.White,
						contentColor = Color.Black,
					),
				) {
					Icon(
						imageVector = Icons.Filled.PlayArrow,
						contentDescription = null,
						tint = Color.Black,
						modifier = Modifier.size(22.dp),
					)
					Spacer(Modifier.width(8.dp))
					Text(
						text = if (isResume) "Resume in  MpvEx" else "Play in MpvEx",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
					)
				}
				
				if (isResume && progressFraction > 0f) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(12.dp),
					) {
						LinearProgressIndicator(
							progress = { progressFraction },
							modifier = Modifier
								.weight(1f)
								.height(4.dp)
								.clip(RoundedCornerShape(2.dp)),
							color = Color(0xFF52B54B),
							trackColor = Color(0xFF333338),
						)
						if (remainingMinutes != null) {
							Text(
								text = "$remainingMinutes m remaining",
								style = MaterialTheme.typography.bodySmall,
								color = Color(0xFFB0B0B8),
							)
						}
					}
				}
				
				item.overview?.takeIf { it.isNotBlank() }?.let { overview ->
					Text(
						text = overview,
						style = MaterialTheme.typography.bodyMedium,
						color = Color(0xFFE0E0E6),
						lineHeight = 22.sp,
					)
				}
				
				if (directors.isNotEmpty()) {
					Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(
							text = "Director:",
							style = MaterialTheme.typography.bodyMedium,
							color = Color(0xFF8E8E93),
						)
						Text(
							text = directors.joinToString(", "),
							style = MaterialTheme.typography.bodyMedium,
							color = Color.White,
						)
					}
				}
				
				if (writers.isNotEmpty()) {
					Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(
							text = "Writers:",
							style = MaterialTheme.typography.bodyMedium,
							color = Color(0xFF8E8E93),
						)
						Text(
							text = writers.joinToString(", "),
							style = MaterialTheme.typography.bodyMedium,
							color = Color.White,
						)
					}
				}
			}
		}
		
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.statusBarsPadding()
				.padding(horizontal = 8.dp, vertical = 4.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			IconButton(
				onClick = onBack,
				modifier = Modifier
					.size(40.dp)
					.clip(CircleShape)
					.background(Color.Black.copy(alpha = 0.4f)),
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Filled.ArrowBack,
					contentDescription = "Back",
					tint = Color.White,
				)
			}
		}
	}
}
