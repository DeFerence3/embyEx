package app.deference.embycl.ui.screens.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.utils.asRuntime
import app.deference.embycl.core.utils.formatToString
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.components.LoadingScaffold
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.screens.details.components.PlaybackEffect
import app.deference.embycl.ui.screens.details.components.adaptive.DesktopDetailsLayout
import app.deference.embycl.ui.screens.details.components.adaptive.ItemDetailsData
import app.deference.embycl.ui.screens.details.components.adaptive.LandscapeDetailsLayout
import app.deference.embycl.ui.screens.details.components.adaptive.PortraitDetailsLayout
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
		val events = viewModel.events
		val onAction = viewModel::onAction
		val onBack = { backStack.goBack() }

		PlaybackEffect(events, onAction)
		LoadingScaffold(
			state = state.content,
            topBar = { if (state.content?.isSuccess != true) DetailTopBar("Details", onBack) },
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
}

@Composable
private fun ItemDetails(
	item: EmbyItem,
	onBack: () -> Unit,
	onPlay: () -> Unit,
) {
	val windowSizeClass =
		currentWindowAdaptiveInfo().windowSizeClass

	val configuration =
		DeviceConfiguration.fromWindowSizeClass(windowSizeClass)

	ItemDetailsContent(
		item = item,
		configuration = configuration,
		onBack = onBack,
		onPlay = onPlay,
	)
}

@Composable
private fun ItemDetailsContent(
	item: EmbyItem,
	configuration: DeviceConfiguration,
	onBack: () -> Unit,
	onPlay: () -> Unit,
) {
	val backdrop =
		item.imageUrl(type = "Backdrop", maxWidth = 1920)
			?: item.imageUrl(type = "Primary", maxWidth = 1280)

	val logoUrl =
		item.imageUrl(type = "Logo", maxWidth = 800)

	val videoStream =
		item.mediaStreams.firstOrNull { it.type == "Video" }

	val videoResolution =
		videoStream?.displayTitle
			?.takeIf { it.isNotBlank() }
			?: item.container?.uppercase()
			?: "HD"

	val audioTitle =
		item.mediaStreams
			.filter { it.type == "Audio" }
			.mapNotNull {
				(it.displayLanguage ?: it.displayTitle)
					?.takeIf(String::isNotBlank)
			}
			.distinct()
			.joinToString(" • ")
			.ifBlank { "Unknown" }

	val subtitleStreams =
		item.mediaStreams
			.filter { it.type == "Subtitle" }
			.mapNotNull {
				(it.displayLanguage ?: it.displayTitle)
					?.takeIf(String::isNotBlank)
			}
			.distinct()
			.joinToString(" • ")
			.ifBlank { "None" }

	val airDate =
		item.premiereDate?.formatToString()
			?: item.productionYear?.toString()

	val runtime =
		item.runTimeTicks?.asRuntime()

	val position =
		item.userData?.playbackPositionTicks ?: 0L

	val duration =
		item.runTimeTicks ?: 0L

	val isResume =
		position > 0L

	val progress =
		if (duration > 0L && position > 0L) {
			(position.toFloat() / duration.toFloat())
				.coerceIn(0f, 1f)
		} else {
			0f
		}

	val remainingMinutes =
		if (duration > position && isResume) {
			((duration - position) /
					10_000_000L /
					60L
					).coerceAtLeast(1)
		} else {
			null
		}

	val directors =
		item.people
			.filter { it.type == "Director" }
			.mapNotNull { it.name }
			.distinct()

	val writers =
		item.people
			.filter { it.type == "Writer" }
			.mapNotNull { it.name }
			.distinct()

	val data = ItemDetailsData(
		title = item.name,
		backdrop = backdrop,
		logo = logoUrl,
		airDate = airDate,
		runtime = runtime,
		resolution = videoResolution,
		audio = audioTitle,
		subtitles = subtitleStreams,
		overview = item.overview,
		directors = directors,
		writers = writers,
		isResume = isResume,
		progress = progress,
		remainingMinutes = remainingMinutes,
	)

	Box(
		modifier = Modifier
			.fillMaxSize(),
	) {

		AnimatedContent(
			targetState = configuration,
			transitionSpec = {
				fadeIn(
					tween(180)
				) togetherWith fadeOut(
					tween(120)
				)
			},
			label = "ItemDetailsAdaptiveLayout",
		) { currentConfiguration ->

			when (currentConfiguration) {

				DeviceConfiguration.MOBILE_PORTRAIT -> {
					PortraitDetailsLayout(
						data = data,
						tablet = false,
						onPlay = onPlay,
					)
				}

				DeviceConfiguration.MOBILE_LANDSCAPE -> {
					LandscapeDetailsLayout(
						data = data,
						tablet = false,
						onPlay = onPlay,
					)
				}

				DeviceConfiguration.TABLET_PORTRAIT -> {
					PortraitDetailsLayout(
						data = data,
						tablet = true,
						onPlay = onPlay,
					)
				}

				DeviceConfiguration.TABLET_LANDSCAPE -> {
					LandscapeDetailsLayout(
						data = data,
						tablet = true,
						onPlay = onPlay,
					)
				}

				DeviceConfiguration.DESKTOP -> {
					DesktopDetailsLayout(
						data = data,
						onPlay = onPlay,
					)
				}
			}
		}

		DetailsBackButton(
			onClick = onBack,
			compact = configuration == DeviceConfiguration.MOBILE_PORTRAIT ||
					configuration == DeviceConfiguration.MOBILE_LANDSCAPE,
		)
	}
}

@Composable
private fun DetailsBackButton(
	onClick: () -> Unit,
	compact: Boolean,
) {
	val interactionSource = remember {
		MutableInteractionSource()
	}

	val hovered by interactionSource.collectIsHoveredAsState()

	val scale by animateFloatAsState(
		targetValue = if (hovered) 1.08f else 1f,
		animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
		label = "backButtonScale",
	)

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.statusBarsPadding()
			.padding(
				horizontal = if (compact) 10.dp else 24.dp,
				vertical = 10.dp,
			),
	) {
		IconButton(
			onClick = onClick,
			interactionSource = interactionSource,
			modifier = Modifier
				.size(48.dp)
				.graphicsLayer {
					scaleX = scale
					scaleY = scale
				}
				.clip(CircleShape)
				.background(
					MaterialTheme.colorScheme.surfaceContainerHigh.copy(
						alpha = if (hovered) 0.98f else 0.92f,
					),
				),
		) {
			Icon(
				imageVector = Icons.AutoMirrored.Filled.ArrowBack,
				contentDescription = "Back"
			)
		}
	}
}
