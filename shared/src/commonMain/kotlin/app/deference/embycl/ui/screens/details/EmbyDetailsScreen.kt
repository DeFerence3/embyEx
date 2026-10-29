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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import app.deference.embycl.core.utils.asRuntime
import app.deference.embycl.core.utils.formatToString
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.core.components.LoadingScaffold
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.screens.details.components.adaptive.DesktopDetailsLayout
import app.deference.embycl.ui.screens.details.components.adaptive.ItemDetailsData
import app.deference.embycl.ui.screens.details.components.adaptive.LandscapeDetailsLayout
import app.deference.embycl.ui.screens.details.components.adaptive.PortraitDetailsLayout
import app.deference.embycl.ui.screens.downloads.components.DownloadSourceDialog
import app.deference.embycl.ui.screens.downloads.rememberDownloadPermissionRequest
import app.deference.embycl.ui.screens.player.EmbyPlayerScreen
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.player_dismiss
import embyex.shared.generated.resources.player_error
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EmbyDetailsScreen(val id: String) : Screen {

	@Composable
	override fun Content() {
		val navigator = LocalNavigator.current
		val viewModel = koinViewModel<EmbyDetailsVM>(parameters = { parametersOf(id) })
		val state by viewModel.state.collectAsStateWithLifecycle()
		val events = viewModel.events
		val onAction = viewModel::onAction
		val onBack = { navigator.goBack() }
		val permission = rememberDownloadPermissionRequest()

        var message by remember { mutableStateOf<String?>(value = null) }
        val playbackError = stringResource(resource = Res.string.player_error)
        events.ObserveEvent { event ->
			message = when (event) {
				is EmbyDetailsEvent.Error -> event.message
				EmbyDetailsEvent.PlaybackFailed -> playbackError
			}
        }
        LifecycleResumeEffect(key1 = id) {
            onAction(EmbyDetailsAction.Retry)
            onPauseOrDispose { }
        }
        if (message != null) {
            AlertDialog(
                onDismissRequest = {
                    message = null
                },
                text = { Text(text = message.orEmpty()) },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = {
                        message = null
                        }) { Text(text = stringResource(resource = Res.string.player_dismiss)) }
                },
            )
        }
		LoadingScaffold(
			state = state.content,
            topBar = { if (state.content?.isSuccess != true) DetailTopBar("Details", onBack) },
			onRetry = { onAction(EmbyDetailsAction.Retry) },
			modifier = Modifier.fillMaxSize(),
			content = { item ->
				ItemDetails(
					item = item,
					onBack = onBack,
					onPlay = dropUnlessResumed { navigator.goTo(destination = EmbyPlayerScreen(id = id)) },
					onDownload = { onAction(EmbyDetailsAction.Download) },
					onPlayFromServer = dropUnlessResumed { navigator.goTo(destination = EmbyPlayerScreen(id = id, fromServer = true)) },
					onRemove = { onAction(EmbyDetailsAction.RemoveDownload) },
				)
			}
		)
		state.choice?.let { choice ->
			DownloadSourceDialog(
				title = choice.title,
				sources = choice.sources,
				selectedKey = choice.selected,
				serverAvailable = true,
				onSelect = { onAction(EmbyDetailsAction.SelectSource(it)) },
				onConfirm = { source ->
					permission { _ -> if (viewModel.state.value.choice?.selected == source.key) onAction(EmbyDetailsAction.ConfirmDownload) }
				},
				onDismiss = { onAction(EmbyDetailsAction.DismissDownload) },
				isSubmitting = choice.submitting,
			)
		}
	}
}

@Composable
fun ItemDetails(
	item: EmbyItem,
	onBack: () -> Unit,
	onPlay: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
	onDownload: () -> Unit = {},
	onPlayFromServer: () -> Unit = {},
	onRemove: () -> Unit = {},
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
        actions = actions,
		onDownload = onDownload,
		onPlayFromServer = onPlayFromServer,
		onRemove = onRemove,
	)
}

@Composable
private fun ItemDetailsContent(
	item: EmbyItem,
	configuration: DeviceConfiguration,
	onBack: () -> Unit,
	onPlay: () -> Unit,
    actions: (@Composable () -> Unit)? = null,
	onDownload: () -> Unit = {},
	onPlayFromServer: () -> Unit = {},
	onRemove: () -> Unit = {},
) {
	val backdrop = (item.imageUrl(type = "Backdrop", maxWidth = 1920) ?: item.imageUrl(type = "Primary", maxWidth = 1280))

	val logoUrl = item.imageUrl(type = "Logo", maxWidth = 800)

    val streams = item.mediaStreams

	val videoStream = streams.firstOrNull { it.type == "Video" }

	val videoResolution =
		videoStream?.displayTitle
			?.takeIf { it.isNotBlank() }
			?: item.container?.uppercase()
			?: "HD"

	val audioTitle =
		streams
			.filter { it.type == "Audio" }
			.mapNotNull {
				(it.displayLanguage ?: it.displayTitle)
					?.takeIf(String::isNotBlank)
			}
			.distinct()
			.joinToString(" • ")
			.ifBlank { "Unknown" }

	val subtitleStreams =
        streams
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
		isAvailableOffline = item.isAvailableOffline
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
                        actions = actions,
						tablet = false,
						onPlay = onPlay,
						onDownload = onDownload,
						onPlayFromServer = onPlayFromServer,
						onRemove = onRemove,
					)
				}

				DeviceConfiguration.MOBILE_LANDSCAPE -> {
					LandscapeDetailsLayout(
						data = data,
                        actions = actions,
						tablet = false,
						onPlay = onPlay,
						onDownload = onDownload,
						onPlayFromServer = onPlayFromServer,
						onRemove = onRemove,
					)
				}

				DeviceConfiguration.TABLET_PORTRAIT -> {
					PortraitDetailsLayout(
						data = data,
                        actions = actions,
						tablet = true,
						onPlay = onPlay,
						onDownload = onDownload,
						onPlayFromServer = onPlayFromServer,
						onRemove = onRemove,
					)
				}

				DeviceConfiguration.TABLET_LANDSCAPE -> {
					LandscapeDetailsLayout(
						data = data,
                        actions = actions,
						tablet = true,
						onPlay = onPlay,
						onDownload = onDownload,
						onPlayFromServer = onPlayFromServer,
						onRemove = onRemove,
					)
				}

				DeviceConfiguration.DESKTOP -> {
					DesktopDetailsLayout(
						data = data,
                        actions = actions,
						onPlay = onPlay,
						onDownload = onDownload,
						onPlayFromServer = onPlayFromServer,
						onRemove = onRemove,
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
