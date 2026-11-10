package app.deference.embycl.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.components.AndroidPlayerSeekFeedback
import app.deference.embycl.ui.player.components.AndroidPlayerSheets
import app.deference.embycl.ui.player.components.AndroidPlayerTile
import app.deference.embycl.ui.player.components.AndroidPlayerUnlock
import app.deference.embycl.ui.player.components.PlayerFeedbackPill
import app.deference.embycl.ui.player.components.PlayerHeading
import app.deference.embycl.ui.player.components.PlayerPill
import app.deference.embycl.ui.player.components.PlayerPlayButton
import app.deference.embycl.ui.player.components.PlayerSidePanel
import app.deference.embycl.ui.player.components.PlayerTimeline
import app.deference.embycl.ui.player.components.PlayerUpNextCard
import app.deference.embycl.ui.player.components.audioLabel
import app.deference.embycl.ui.player.components.label
import app.deference.embycl.ui.player.components.speedLabel
import app.deference.embycl.ui.player.components.subtitleLabel
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_back
import embyex.player.generated.resources.player_forward
import embyex.player.generated.resources.player_hide_chapters
import embyex.player.generated.resources.player_lock
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_next_episode
import embyex.player.generated.resources.player_previous_episode
import embyex.player.generated.resources.player_rotate
import embyex.player.generated.resources.player_show_chapters
import embyex.player.generated.resources.player_speed_value
import embyex.player.generated.resources.player_subtitles
import embyex.player.generated.resources.player_subtitles_off
import embyex.player.generated.resources.player_tile_aspect
import embyex.player.generated.resources.player_tile_audio
import embyex.player.generated.resources.player_tile_lock
import embyex.player.generated.resources.player_tile_more
import embyex.player.generated.resources.player_tile_pip
import embyex.player.generated.resources.player_tile_rotate
import embyex.player.generated.resources.player_tile_speed
import embyex.player.generated.resources.player_tile_subtitles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

/** Tablets in landscape keep the panel open beside the picture by default. */
private val WIDE_SCREEN = 1100.dp


/**
 * Android player. Portrait puts the picture on top, chapters / Up next in the middle and the controls within
 * thumb reach; nothing covers the picture, so the controls never hide. Landscape and tablets draw the same
 * dock over the full-screen picture and hide it after 3 s. Gestures, lock and sheets work as before.
 */
@Composable
internal fun AndroidPlayerOverlay(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    onOpenExternalPlayer: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    queue: PlayerQueueControls?,
    subtitleStyle: PlayerSubtitleStyleControls,
    surface: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.playerActivity()
    val accessibility = LocalAccessibilityManager.current
    var visible by remember { mutableStateOf(value = true) }
    var locked by remember { mutableStateOf(value = false) }
    var pressed by remember { mutableStateOf(value = false) }
    var controlsFocused by remember { mutableStateOf(value = false) }
    var keyboardInput by remember { mutableStateOf(value = false) }
    val focus = remember { FocusRequester() }
    var seeking by remember { mutableStateOf(value = false) }
    var interaction by remember { mutableIntStateOf(value = 0) }
    var sheet by remember { mutableStateOf<AndroidPlayerSheet?>(value = null) }
    var feedback by remember { mutableStateOf<PlayerFeedback?>(value = null) }
    var feedbackVersion by remember { mutableIntStateOf(value = 0) }
    var seekSeconds by remember { mutableIntStateOf(value = 0) }
    var seekVersion by remember { mutableIntStateOf(value = 0) }
    var panelChoice by rememberSaveable { mutableStateOf<Boolean?>(value = null) }
    var panelTab by rememberSaveable { mutableStateOf(value = PlayerPanelTab.Chapters) }
    val chapters by (player?.chapters ?: remember { MutableStateFlow(value = emptyList<PlayerChapter>()) }).collectAsState()
    val chapterIndex by remember(player, chapters) {
        (player?.currentPositionMillis ?: MutableStateFlow(value = 0L))
            .map { position -> chapters.indexOfLast { it.startMillis <= position } }
            .distinctUntilChanged()
    }.collectAsState(initial = -1)
    val upNextCountdown by rememberUpNextCountdown(player = player, chapters = chapters)
    val upNext = queue?.queue?.next?.takeIf { !queue.cancelled && upNextCountdown != null && !failed }
    UpNextAutoplayEffect(
        countdown = upNextCountdown,
        upNext = upNext,
        autoplay = queue?.autoplay == true,
        onNavigate = { entry -> queue?.onNavigate(entry) },
    )
    val hasPanel = chapters.isNotEmpty() || queue != null
    val playing = player?.state?.collectAsState()?.value?.playWhenReady == true
    val enterPictureInPicture: (() -> Unit)? = if (window.supportsPictureInPicture) window.togglePictureInPicture else null
    val previous = queue?.queue?.previous?.let { entry -> { queue.onNavigate(entry) } }
    val next = queue?.queue?.next?.let { entry -> { queue.onNavigate(entry) } }
    val currentChapter = chapters.getOrNull(index = chapterIndex)?.let { (chapterIndex + 1) to it.name }
    val configuration = LocalConfiguration.current
    // The panel's default before the user chooses: open in portrait and on wide screens.
    val panelDefault = configuration.orientation == Configuration.ORIENTATION_PORTRAIT || configuration.screenWidthDp >= WIDE_SCREEN.value

    DisposableEffect(key1 = activity) {
        val orientation = activity.requestedOrientation
        onDispose { activity.requestedOrientation = orientation }
    }
    LaunchedEffect(key1 = paused, key2 = failed, key3 = window.isPictureInPicture) { visible = true }
    LaunchedEffect(key1 = visible, key2 = window.isPictureInPicture) {
        if (!visible && sheet == null && !window.isPictureInPicture) focus.requestFocus()
    }
    LaunchedEffect(key1 = feedbackVersion) {
        if (feedback != null) {
            delay(timeMillis = 1_300L)
            feedback = null
        }
    }
    LaunchedEffect(key1 = seekVersion) {
        if (seekSeconds != 0) {
            delay(timeMillis = 1_300L)
            seekSeconds = 0
        }
    }

    // Hardware keyboards get the same shortcuts as desktop. There is no player volume on Android, so ↑ ↓ and M
    // report "Not available here"; the device's volume keys still work.
    RegisterPlayerShortcuts(
        player = player.takeUnless { failed || locked },
        enabled = !window.isPictureInPicture && sheet == null && !locked,
        focusFree = !controlsFocused,
        controlsHidden = !visible,
        actions = PlayerShortcutActions(
            onEscape = { if (upNext != null) queue.onCancelUpNext() else onClose() },
            onOpenMenu = { menu ->
                sheet = when (menu) {
                    PlayerMenu.Subtitles -> AndroidPlayerSheet.Subtitles
                    PlayerMenu.Audio -> AndroidPlayerSheet.Audio
                    PlayerMenu.Speed -> AndroidPlayerSheet.Speed
                    PlayerMenu.Aspect -> AndroidPlayerSheet.Aspect
                    PlayerMenu.More -> AndroidPlayerSheet.More
                    PlayerMenu.SubtitleStyle,
                    PlayerMenu.SubtitleStyleFromMore,
                    -> AndroidPlayerSheet.SubtitleStyle
                }
            },
            onTogglePanel = if (hasPanel) { { panelChoice = !(panelChoice ?: panelDefault); visible = true } } else null,
            onToggleFullscreen = null,
            onPictureInPicture = enterPictureInPicture,
            onRevealControls = { visible = true },
            onPreviousEpisode = previous,
            onNextEpisode = next,
            hasQueue = queue != null,
            onPlayUpNext = upNext?.let { entry -> { queue.onNavigate(entry) } },
        ),
        onTriggered = {
            keyboardInput = true
            interaction++
        },
        onFeedback = { feedback = it; feedbackVersion++ },
        dispatchAction = dispatchAction,
    )

    BackHandler(enabled = locked && !window.isPictureInPicture) { visible = true }
    val slot = remember { PlayerPictureSlot() }
    val onSheet: (AndroidPlayerSheet) -> Unit = { sheet = it; visible = true }
    val lockControls: () -> Unit = { locked = true; visible = true }

    BoxWithConstraints(
        modifier = modifier
            .background(color = PlayerColors.background)
            .pointerInput(key1 = Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        interaction++
                        keyboardInput = false
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .focusRequester(focusRequester = focus)
            .focusable()
            .then(other = with(receiver = slot) { Modifier.root() }),
    ) {
        val portrait = maxHeight > maxWidth
        // The one video surface for every layout, PiP included, so no switch reloads the video; see PlayerPictureSlot.
        surface(with(receiver = slot) { Modifier.follow() })
        if (window.isPictureInPicture) {
            // System PiP draws its own controls; the picture fills the window.
            Box(modifier = Modifier.fillMaxSize().then(other = with(receiver = slot) { Modifier.slot() }))
            return@BoxWithConstraints
        }
        val wide = maxWidth >= WIDE_SCREEN
        val rotate: () -> Unit = {
            activity.requestedOrientation = if (portrait) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
        // Portrait never covers the picture, so its controls stay; landscape hides them after a pause.
        val keepVisible = portrait || (!locked && (paused || failed || player == null)) || pressed || seeking ||
            sheet != null || (controlsFocused && keyboardInput)
        LaunchedEffect(visible, keepVisible, interaction, locked) {
            if (visible && !keepVisible) {
                delay(timeMillis = accessibility?.calculateRecommendedTimeoutMillis(
                    originalTimeoutMillis = if (locked) 2_000L else 3_000L,
                    containsIcons = true,
                    containsText = true,
                    containsControls = true,
                ) ?: 3_000L)
                visible = false
            }
        }
        val gestures: @Composable (Modifier) -> Unit = { gestureModifier ->
            AndroidPlayerGestures(
                player = player,
                enabled = !failed && !locked,
                onTap = { if (!portrait || locked) visible = !visible },
                onSeek = { seconds ->
                    seekSeconds = if ((seekSeconds > 0) == (seconds > 0)) seekSeconds + seconds else seconds
                    seekVersion++
                },
                onFeedback = { feedback = it; feedbackVersion++ },
                onDragging = { seeking = it },
                dispatchAction = dispatchAction,
                modifier = gestureModifier,
            )
        }
        val timeline: @Composable () -> Unit = {
            if (player != null) PlayerPill(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                PlayerTimeline(
                    player = player,
                    enabled = !failed,
                    onSeeking = { seeking = it },
                    dispatchAction = dispatchAction,
                    chapters = chapters,
                    wavy = playing,
                )
            }
        }
        val transport: @Composable (Modifier) -> Unit = { transportModifier ->
            if (player != null) TransportPill(
                player = player,
                enabled = !failed,
                episodes = queue != null,
                onPrevious = previous,
                onNext = next,
                dispatchAction = dispatchAction,
                modifier = transportModifier,
            )
        }
        val panel: @Composable (Modifier) -> Unit = { panelModifier ->
            PlayerSidePanel(
                tab = panelTab,
                onTabChange = { panelTab = it },
                chapters = chapters,
                currentChapter = chapterIndex,
                onChapterClick = { chapter -> player?.let { active -> dispatchAction { active.seekTo(positionMillis = chapter.startMillis) } } },
                upcoming = queue?.queue?.upcoming,
                autoplay = queue?.autoplay == true,
                onAutoplayChange = { queue?.onAutoplayChange(it) },
                onEntryClick = { entry -> queue?.onNavigate(entry) },
                onClose = { panelChoice = false },
                modifier = panelModifier,
            )
        }
        val upNextCard: @Composable (Modifier) -> Unit = { cardModifier ->
            if (upNext != null) PlayerUpNextCard(
                entry = upNext,
                secondsLeft = upNextCountdown?.secondsLeft?.takeIf { queue.autoplay },
                onPlayNow = { queue.onNavigate(upNext) },
                onCancel = queue.onCancelUpNext,
                modifier = cardModifier,
            )
        }
        val controlsModifier = Modifier
            .onFocusChanged { controlsFocused = it.hasFocus }
            .focusGroup()

        val pictureLayers: @Composable () -> Unit = {
            gestures(Modifier.fillMaxSize())
            AndroidPlayerSeekFeedback(seconds = seekSeconds, trigger = seekVersion, modifier = Modifier.fillMaxSize())
        }
        if (portrait) {
            val showPanel = hasPanel && (panelChoice ?: true) && !locked
            val panelSpring by animateFloatAsState(
                targetValue = if (showPanel) 1f else 0f,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            )
            val panelShown = panelSpring.coerceIn(minimumValue = 0f, maximumValue = 1f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing),
            ) {
                if (!locked) CompositionLocalProvider(LocalContentColor provides PlayerColors.content) {
                    PlayerHeading(
                        title = source.title,
                        subtitle = source.subtitle,
                        chapter = if (showPanel) null else currentChapter,
                        onClose = onClose,
                        modifier = controlsModifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        trailing = if (hasPanel && !showPanel) {
                            {
                                PlayerIconButton(
                                    icon = Icons.AutoMirrored.Filled.ViewSidebar,
                                    description = stringResource(resource = Res.string.player_show_chapters),
                                    onClick = { panelChoice = true },
                                    shortcut = "P",
                                )
                            }
                        } else null,
                    )
                }
                // The panel's share of the free space animates; the picture slides between top and centre.
                Column(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .fillMaxWidth(),
                ) {
                    Spacer(modifier = Modifier.weight(weight = (1f - panelShown) / 2f + 0.0001f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio = 16f / 9f)
                            .then(other = with(receiver = slot) { Modifier.slot() }),
                    ) {
                        pictureLayers()
                    }
                    if (!locked) upNextCard(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                    Box(
                        modifier = Modifier
                            .weight(weight = panelShown + 0.0001f)
                            .fillMaxWidth()
                            .clipToBounds()
                            .graphicsLayer { alpha = panelShown },
                    ) {
                        if (panelShown > 0.01f) panel(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    Spacer(modifier = Modifier.weight(weight = (1f - panelShown) / 2f + 0.0001f))
                }
                if (!locked) Column(
                    modifier = controlsModifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    timeline()
                    transport(Modifier.fillMaxWidth())
                    if (player != null) PortraitTiles(
                        player = player,
                        enabled = !failed,
                        onSheet = onSheet,
                        onLock = lockControls,
                        onRotate = rotate,
                        onPictureInPicture = enterPictureInPicture,
                    )
                }
            }
            FeedbackHost(feedback = feedback)
        } else {
            val open = hasPanel && (panelChoice ?: wide) && !locked
            // The expressive spring overshoots; padding must not go negative.
            val pictureEndSpring by animateDpAsState(
                targetValue = if (open && visible) 312.dp else 0.dp,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            )
            val pictureEnd = pictureEndSpring.coerceAtLeast(minimumValue = 0.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = pictureEnd)
                    .then(other = with(receiver = slot) { Modifier.slot() }),
            ) {
                pictureLayers()
            }
            AnimatedVisibility(
                visible = visible && !locked && sheet == null,
                enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = 120.dp)
                            .background(brush = Brush.verticalGradient(0f to PlayerColors.controlsScrim, 1f to PlayerColors.transparent)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                            .then(other = controlsModifier),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .fillMaxHeight()
                                .padding(start = 20.dp, end = if (open) 12.dp else 20.dp, top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides PlayerColors.content) {
                                PlayerHeading(
                                    title = source.title,
                                    subtitle = source.subtitle,
                                    chapter = if (open) null else currentChapter,
                                    onClose = onClose,
                                )
                            }
                            Spacer(modifier = Modifier.weight(weight = 1f))
                            timeline()
                            if (player != null) LandscapeRow(
                                player = player,
                                enabled = !failed,
                                panelOpen = if (hasPanel) open else null,
                                onTogglePanel = { panelChoice = !open },
                                onSheet = onSheet,
                                onLock = lockControls,
                                onRotate = rotate,
                                transport = transport,
                            )
                        }
                        AnimatedVisibility(
                            visible = open,
                            enter = slideInHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeIn(),
                            exit = slideOutHorizontally(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { it } + fadeOut(),
                        ) {
                            panel(
                                Modifier
                                    .width(width = 288.dp)
                                    .fillMaxHeight()
                                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                            )
                        }
                    }
                }
            }
            FeedbackHost(feedback = feedback)
            upNextCard(
                Modifier
                    .align(alignment = Alignment.BottomEnd)
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                    .padding(end = pictureEnd + 20.dp, bottom = 132.dp),
            )
        }
        if (locked && visible) AndroidPlayerUnlock(
            onUnlock = { locked = false; visible = true },
            onDragging = { seeking = it },
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                .padding(bottom = 24.dp),
        )
        val currentSheet = sheet
        if (currentSheet != null && player != null) AndroidPlayerSheets(
            sheet = currentSheet,
            player = player,
            onDismiss = { sheet = null; visible = true; interaction++ },
            onSheet = onSheet,
            onOpenExternalPlayer = onOpenExternalPlayer,
            // Portrait already has an Aspect tile.
            showAspect = !portrait,
            subtitleStyle = subtitleStyle,
            dispatchAction = dispatchAction,
        )
    }
}

@Composable
private fun FeedbackHost(
    feedback: PlayerFeedback?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(insets = WindowInsets.safeDrawing)
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (feedback != null) PlayerFeedbackPill(feedback = feedback)
    }
}

@Composable
private fun TransportPill(
    player: EmbeddedPlayer,
    enabled: Boolean,
    episodes: Boolean,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by player.state.collectAsState()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PlayerPill(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) {
            if (episodes) PlayerIconButton(
                icon = Icons.Default.SkipPrevious,
                description = stringResource(resource = Res.string.player_previous_episode),
                onClick = { onPrevious?.invoke() },
                enabled = enabled && onPrevious != null,
                tonal = false,
            )
            PlayerIconButton(
                icon = Icons.Default.Replay10,
                description = stringResource(resource = Res.string.player_back),
                onClick = { dispatchAction { player.skip(deltaMillis = -10_000L) } },
                enabled = enabled,
                tonal = false,
            )
            PlayerPlayButton(
                paused = !state.playWhenReady,
                buffering = state.isLoadingOrBuffering,
                enabled = enabled,
                onClick = { dispatchAction { player.togglePlayWhenReady() } },
                size = 56.dp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            PlayerIconButton(
                icon = Icons.Default.Forward10,
                description = stringResource(resource = Res.string.player_forward),
                onClick = { dispatchAction { player.skip(deltaMillis = 10_000L) } },
                enabled = enabled,
                tonal = false,
            )
            if (episodes) PlayerIconButton(
                icon = Icons.Default.SkipNext,
                description = stringResource(resource = Res.string.player_next_episode),
                onClick = { onNext?.invoke() },
                enabled = enabled && onNext != null,
                tonal = false,
            )
        }
    }
}

/** Eight labelled tiles in two rows; each shows its current value where it has one. */
@Composable
private fun PortraitTiles(
    player: EmbeddedPlayer,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    onPictureInPicture: (() -> Unit)?,
) {
    val subtitle = player.subtitleTracks?.selected?.collectAsState()?.value
    val audio = player.audioTracks?.selected?.collectAsState()?.value
    val speed = player.playbackSpeed?.let { it.valueFlow.collectAsState(initial = it.value).value }
    val aspect = player.aspectRatio?.mode?.collectAsState()?.value
    val off = stringResource(resource = Res.string.player_subtitles_off)
    val tiles: List<@Composable (Modifier) -> Unit> = listOf(
        { m -> AndroidPlayerTile(icon = Icons.Default.Subtitles, label = stringResource(resource = Res.string.player_tile_subtitles), value = subtitle?.let(::subtitleLabel) ?: off, enabled = enabled && player.subtitleTracks != null, onClick = { onSheet(AndroidPlayerSheet.Subtitles) }, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.Audiotrack, label = stringResource(resource = Res.string.player_tile_audio), value = audio?.let(::audioLabel), enabled = enabled && player.audioTracks != null, onClick = { onSheet(AndroidPlayerSheet.Audio) }, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.Speed, label = stringResource(resource = Res.string.player_tile_speed), value = speed?.let { speedLabel(speed = it) }, enabled = enabled && speed != null, onClick = { onSheet(AndroidPlayerSheet.Speed) }, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.AspectRatio, label = stringResource(resource = Res.string.player_tile_aspect), value = aspect?.let { stringResource(resource = it.label()) }, enabled = enabled && aspect != null, onClick = { onSheet(AndroidPlayerSheet.Aspect) }, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.Lock, label = stringResource(resource = Res.string.player_tile_lock), enabled = enabled, onClick = onLock, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.ScreenRotation, label = stringResource(resource = Res.string.player_tile_rotate), onClick = onRotate, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.PictureInPictureAlt, label = stringResource(resource = Res.string.player_tile_pip), enabled = enabled && onPictureInPicture != null, onClick = { onPictureInPicture?.invoke() }, modifier = m) },
        { m -> AndroidPlayerTile(icon = Icons.Default.MoreHoriz, label = stringResource(resource = Res.string.player_tile_more), onClick = { onSheet(AndroidPlayerSheet.More) }, modifier = m) },
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        tiles.chunked(size = 4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                row.forEach { tile -> tile(Modifier.weight(weight = 1f)) }
            }
        }
    }
}

/** Lock, rotate and speed left; transport in the middle; tracks, panel and more right. */
@Composable
private fun LandscapeRow(
    player: EmbeddedPlayer,
    enabled: Boolean,
    panelOpen: Boolean?,
    onTogglePanel: () -> Unit,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onLock: () -> Unit,
    onRotate: () -> Unit,
    transport: @Composable (Modifier) -> Unit,
) {
    val speed = player.playbackSpeed?.let { it.valueFlow.collectAsState(initial = it.value).value }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Box(modifier = Modifier.weight(weight = 1f), contentAlignment = Alignment.CenterStart) {
            PlayerPill(contentPadding = PaddingValues(all = 4.dp)) {
                PlayerIconButton(icon = Icons.Default.Lock, description = stringResource(resource = Res.string.player_lock), onClick = onLock, enabled = enabled, tonal = false)
                PlayerIconButton(icon = Icons.Default.ScreenRotation, description = stringResource(resource = Res.string.player_rotate), onClick = onRotate, tonal = false)
                if (speed != null) PlayerIconButton(
                    icon = Icons.Default.Speed,
                    description = stringResource(Res.string.player_speed_value, speedLabel(speed = speed)),
                    onClick = { onSheet(AndroidPlayerSheet.Speed) },
                    enabled = enabled,
                    tonal = false,
                )
            }
        }
        transport(Modifier)
        Box(modifier = Modifier.weight(weight = 1f), contentAlignment = Alignment.CenterEnd) {
            PlayerPill(contentPadding = PaddingValues(all = 4.dp)) {
                if (player.subtitleTracks != null) PlayerIconButton(icon = Icons.Default.Subtitles, description = stringResource(resource = Res.string.player_subtitles), onClick = { onSheet(AndroidPlayerSheet.Subtitles) }, enabled = enabled, shortcut = "C", tonal = false)
                if (player.audioTracks != null) PlayerIconButton(icon = Icons.Default.Audiotrack, description = stringResource(resource = Res.string.player_audio), onClick = { onSheet(AndroidPlayerSheet.Audio) }, enabled = enabled, shortcut = "A", tonal = false)
                if (panelOpen != null) PlayerIconButton(
                    icon = Icons.AutoMirrored.Filled.ViewSidebar,
                    description = stringResource(resource = if (panelOpen) Res.string.player_hide_chapters else Res.string.player_show_chapters),
                    onClick = onTogglePanel,
                    shortcut = "P",
                    tonal = false,
                    selected = panelOpen,
                )
                PlayerIconButton(icon = Icons.Default.MoreVert, description = stringResource(resource = Res.string.player_more), onClick = { onSheet(AndroidPlayerSheet.More) }, tonal = false)
            }
        }
    }
}
