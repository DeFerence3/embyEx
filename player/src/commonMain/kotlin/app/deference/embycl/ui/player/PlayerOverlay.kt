package app.deference.embycl.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.components.PlayerSidePanel
import app.deference.embycl.ui.player.components.PlayerUpNextCard
import app.deference.embycl.ui.player.components.PlayerDock
import app.deference.embycl.ui.player.components.PlayerFeedbackPill
import app.deference.embycl.ui.player.components.PlayerHeading
import app.deference.embycl.ui.player.components.PlayerPill
import app.deference.embycl.ui.player.components.PlayerPlayButton
import app.deference.embycl.ui.player.components.PlayerTimeline
import app.deference.embycl.ui.player.components.playerTime
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_back
import embyex.player.generated.resources.player_close
import embyex.player.generated.resources.player_exit_fullscreen
import embyex.player.generated.resources.player_forward
import embyex.player.generated.resources.player_fullscreen
import embyex.player.generated.resources.player_pip_error
import embyex.player.generated.resources.player_restore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

/** Below this width the panel opens over the picture instead of beside it. */
private val WIDE_WINDOW = 1100.dp

/** The Up next card shows for the last this many seconds of an episode. */
private const val UP_NEXT_SECONDS = 10

/** Below this width the dock folds the volume slider, Speed and PiP away. */
private val COMPACT_WINDOW = 940.dp

/**
 * Desktop (and iOS) player chrome around [surface]. Three layouts:
 * - windowed: picture card beside the chapters panel, controls below it; nothing covers the picture and
 *   the controls never hide,
 * - fullscreen: edge to edge; the controls hide after 3 s and only a short scrim sits behind the heading,
 * - PiP: a small dock over the picture, shown on hover or focus.
 */
@Composable
internal fun PlayerOverlay(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    queue: PlayerQueueControls?,
    surface: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(value = true) }
    var interaction by remember { mutableIntStateOf(value = 0) }
    var pressed by remember { mutableStateOf(value = false) }
    var menu by remember { mutableStateOf<PlayerMenu?>(value = null) }
    var seeking by remember { mutableStateOf(value = false) }
    var controlsFocused by remember { mutableStateOf(value = false) }
    var keyboardInput by remember { mutableStateOf(value = false) }
    var feedback by remember { mutableStateOf<PlayerFeedback?>(value = null) }
    var feedbackVersion by remember { mutableIntStateOf(value = 0) }
    // null follows the window width: open when wide in windowed mode, closed in fullscreen.
    var panelChoice by rememberSaveable { mutableStateOf<Boolean?>(value = null) }
    val accessibility = LocalAccessibilityManager.current
    val systemPip = window.isPictureInPicture && window.usesSystemPictureInPicture
    val pip = window.isPictureInPicture && !systemPip
    val fullscreen = window.isFullscreen && !window.isPictureInPicture
    val hideable = pip || fullscreen
    val chapters by (player?.chapters ?: remember { MutableStateFlow(value = emptyList<PlayerChapter>()) }).collectAsState()
    val chapterIndex by remember(player, chapters) {
        (player?.currentPositionMillis ?: MutableStateFlow(value = 0L))
            .map { position -> chapters.indexOfLast { it.startMillis <= position } }
            .distinctUntilChanged()
    }.collectAsState(initial = -1)
    val playing = player?.state?.collectAsState()?.value?.playWhenReady == true
    var panelTab by rememberSaveable { mutableStateOf(value = PlayerPanelTab.Chapters) }
    val hasPanel = chapters.isNotEmpty() || queue != null
    // Seconds until the end, only for the last stretch of a long-enough episode; drives the Up next card.
    val secondsLeft by remember(player) {
        val active = player
        if (active == null) MutableStateFlow(value = null) else combine(active.currentPositionMillis, active.mediaProperties) { position, properties ->
            val duration = properties?.durationMillis ?: 0L
            ((duration - position) / 1_000L).toInt().takeIf { duration > 60_000L && it in 0..UP_NEXT_SECONDS }
        }.distinctUntilChanged()
    }.collectAsState(initial = null)
    val upNext = queue?.queue?.next?.takeIf { !queue.cancelled && secondsLeft != null && !failed }
    val keepVisible = !hideable || paused || failed || player == null || pressed || seeking || menu != null || (controlsFocused && keyboardInput)
    val fullscreenDescription = stringResource(
        resource = if (window.isFullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen,
    )
    val toggleFullscreen: (() -> Unit)? = if (window.supportsFullscreen && !window.isPictureInPicture) window.toggleFullscreen else null
    val enterPip: (() -> Unit)? = if (window.supportsPictureInPicture && !window.isPictureInPicture) window.togglePictureInPicture else null
    val togglePlay: () -> Unit = { player?.let { active -> dispatchAction { active.togglePlayWhenReady() } } }

    fun panelOpen(wide: Boolean): Boolean = hasPanel && (panelChoice ?: (wide && !fullscreen))

    RegisterPlayerShortcuts(
        player = player.takeUnless { failed },
        enabled = menu == null && !systemPip,
        focusFree = !controlsFocused,
        controlsHidden = hideable && !visible,
        actions = PlayerShortcutActions(
            onEscape = {
                when {
                    upNext != null -> queue.onCancelUpNext()
                    pip -> window.togglePictureInPicture()
                    window.isFullscreen && window.supportsFullscreen -> window.toggleFullscreen()
                    else -> onClose()
                }
            },
            onOpenMenu = { menu = it; visible = true },
            onTogglePanel = if (hasPanel && !pip) {
                { panelChoice = !(panelChoice ?: (!fullscreen)); visible = true }
            } else null,
            onToggleFullscreen = toggleFullscreen,
            onPictureInPicture = if (window.supportsPictureInPicture) window.togglePictureInPicture else null,
            onRevealControls = { visible = true },
            onPreviousEpisode = queue?.queue?.previous?.let { entry -> { queue.onNavigate(entry) } },
            onNextEpisode = queue?.queue?.next?.let { entry -> { queue.onNavigate(entry) } },
            hasQueue = queue != null,
            onPlayUpNext = upNext?.let { entry -> { queue.onNavigate(entry) } },
        ),
        onTriggered = {
            interaction++
            keyboardInput = true
        },
        onFeedback = {
            feedback = it
            feedbackVersion++
        },
        dispatchAction = dispatchAction,
    )

    LaunchedEffect(key1 = paused, key2 = failed, key3 = window.isPictureInPicture) {
        visible = true
    }
    LaunchedEffect(key1 = window.isFullscreen, key2 = window.isPictureInPicture) {
        visible = true
        menu = null
        seeking = false
    }
    LaunchedEffect(key1 = interaction, key2 = keepVisible, key3 = visible) {
        if (visible && !keepVisible) {
            delay(timeMillis = accessibility?.calculateRecommendedTimeoutMillis(
                originalTimeoutMillis = 3_000L,
                containsIcons = true,
                containsText = true,
                containsControls = true,
            ) ?: 3_000L)
            visible = false
        }
    }
    LaunchedEffect(key1 = feedbackVersion) {
        if (feedback != null) {
            delay(timeMillis = 1_300L)
            feedback = null
        }
    }

    // Click toggles playback, double click toggles fullscreen. Buttons consume their own clicks first.
    val pictureGestures = Modifier.pointerInput(toggleFullscreen, player) {
        detectTapGestures(
            onTap = { togglePlay(); visible = true },
            onDoubleTap = { toggleFullscreen?.invoke() },
        )
    }
    val controlsModifier = Modifier
        .onFocusChanged { controlsFocused = it.hasFocus }
        .focusGroup()
    val timeline: @Composable (Modifier) -> Unit = { timelineModifier ->
        if (player != null) PlayerPill(
            modifier = timelineModifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
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
    val dock: @Composable (Boolean, Boolean?) -> Unit = { compact, panel ->
        if (player != null) PlayerDock(
            player = player,
            enabled = !failed,
            menu = menu,
            onMenuChange = { menu = it; interaction++ },
            panelOpen = panel,
            onTogglePanel = { panelChoice = !(panel ?: false) },
            fullscreen = window.isFullscreen,
            onToggleFullscreen = toggleFullscreen,
            onPictureInPicture = enterPip,
            dispatchAction = dispatchAction,
            compact = compact,
            onPreviousEpisode = queue?.queue?.previous?.let { entry -> { queue.onNavigate(entry) } },
            onNextEpisode = queue?.queue?.next?.let { entry -> { queue.onNavigate(entry) } },
            episodes = queue != null,
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
            modifier = panelModifier,
        )
    }
    val upNextCard: @Composable (Modifier) -> Unit = { cardModifier ->
        if (upNext != null) PlayerUpNextCard(
            entry = upNext,
            secondsLeft = secondsLeft.takeIf { queue.autoplay },
            onPlayNow = { queue.onNavigate(upNext) },
            onCancel = queue.onCancelUpNext,
            modifier = cardModifier,
        )
    }
    val currentChapter = chapters.getOrNull(index = chapterIndex)?.let { (chapterIndex + 1) to it.name }

    Box(
        modifier = modifier
            .background(color = if (hideable) PlayerColors.background else MaterialTheme.colorScheme.background)
            .pointerInput(key1 = Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        if (event.type != PointerEventType.Exit) {
                            interaction++
                            keyboardInput = false
                        }
                        if (event.type == PointerEventType.Move && event.changes.any {
                                it.type == PointerType.Mouse && !it.pressed && it.position != it.previousPosition
                            }) {
                            visible = true
                        }
                        pressed = event.changes.any { it.pressed }
                    }
                }
            }
            .semantics {
                if (toggleFullscreen != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(
                            label = fullscreenDescription,
                            action = { toggleFullscreen(); true },
                        ),
                    )
                }
            },
    ) {
        when {
            systemPip -> surface(Modifier.fillMaxSize())
            pip -> PipLayout(
                player = player,
                visible = visible,
                failed = failed,
                onRestore = window.togglePictureInPicture,
                onClose = onClose,
                dispatchAction = dispatchAction,
                window = window,
                pictureGestures = pictureGestures,
                surface = surface,
                controlsModifier = controlsModifier,
            )
            fullscreen -> {
                val open = panelOpen(wide = true)
                surface(
                    Modifier
                        .fillMaxSize()
                        .padding(end = if (open && visible) 340.dp else 0.dp)
                        .then(other = pictureGestures),
                )
                PlayerVideoTheme {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(height = 160.dp)
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
                                        .padding(start = 24.dp, end = if (open) 16.dp else 24.dp, top = 16.dp, bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(space = 12.dp),
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
                                    PipErrorLine(window = window)
                                    timeline(Modifier)
                                    dock(false, if (hasPanel) open else null)
                                }
                                if (open) panel(
                                    Modifier
                                        .width(width = 304.dp)
                                        .fillMaxHeight()
                                        .padding(top = 20.dp, bottom = 24.dp, end = 20.dp),
                                )
                            }
                        }
                    }
                    FeedbackHost(feedback = feedback)
                    upNextCard(
                        Modifier
                            .align(alignment = Alignment.BottomEnd)
                            .padding(end = if (open && visible) 364.dp else 24.dp, bottom = 184.dp),
                    )
                }
            }
            else -> BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(insets = WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                val wide = maxWidth >= WIDE_WINDOW
                val compact = maxWidth < COMPACT_WINDOW
                val open = panelOpen(wide = wide)
                val properties = player?.mediaProperties?.collectAsState()?.value
                val width = properties?.videoWidth ?: 0
                val height = properties?.videoHeight ?: 0
                val ratio = if (width > 0 && height > 0) width.toFloat() / height else 16f / 9f
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                    ) {
                        PlayerHeading(
                            title = source.title,
                            subtitle = source.subtitle,
                            chapter = if (open) null else currentChapter,
                            onClose = onClose,
                            modifier = controlsModifier,
                        )
                        Box(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            surface(
                                Modifier
                                    .aspectRatio(ratio = ratio)
                                    .clip(shape = MaterialTheme.shapes.extraLarge)
                                    .then(other = pictureGestures),
                            )
                            FeedbackHost(feedback = feedback)
                            upNextCard(
                                Modifier
                                    .align(alignment = Alignment.BottomEnd)
                                    .padding(all = 16.dp),
                            )
                        }
                        Column(
                            modifier = controlsModifier,
                            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                        ) {
                            PipErrorLine(window = window)
                            timeline(Modifier)
                            dock(compact, if (hasPanel) open else null)
                        }
                    }
                    if (open && wide) panel(
                        Modifier
                            .width(width = 304.dp)
                            .fillMaxHeight(),
                    )
                }
                if (open && !wide) panel(
                    Modifier
                        .align(alignment = Alignment.TopEnd)
                        .width(width = 280.dp)
                        .padding(bottom = 148.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun FeedbackHost(
    feedback: PlayerFeedback?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (feedback != null) PlayerFeedbackPill(feedback = feedback)
    }
}

@Composable
private fun PipErrorLine(
    window: PlayerWindowState,
) {
    if (window.pictureInPictureFailed) Text(
        text = stringResource(resource = Res.string.player_pip_error),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun BoxScope.PipLayout(
    player: EmbeddedPlayer?,
    visible: Boolean,
    failed: Boolean,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    window: PlayerWindowState,
    pictureGestures: Modifier,
    surface: @Composable (Modifier) -> Unit,
    controlsModifier: Modifier,
) {
    val density = LocalDensity.current.density
    surface(
        Modifier
            .fillMaxSize()
            .then(other = pictureGestures)
            .pointerInput(window, density) {
                // Undecorated hosts move the window by dragging the picture; decorated ones ignore it.
                detectDragGestures(
                    onDragStart = { window.startMove() },
                    onDrag = { change, amount ->
                        change.consume()
                        window.moveBy(amount.x / density, amount.y / density)
                    },
                )
            },
    )
    VideoThemeBox {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().then(other = controlsModifier)) {
                val minimal = maxWidth < 400.dp
                PlayerPill(
                    modifier = Modifier
                        .align(alignment = Alignment.TopEnd)
                        .padding(all = 8.dp),
                    contentPadding = PaddingValues(all = 4.dp),
                ) {
                    PlayerIconButton(
                        icon = Icons.Default.OpenInFull,
                        description = stringResource(resource = Res.string.player_restore),
                        onClick = onRestore,
                        size = 40.dp,
                        shortcut = "Esc",
                        tonal = false,
                    )
                    if (!minimal) PlayerIconButton(
                        icon = Icons.Default.Close,
                        description = stringResource(resource = Res.string.player_close),
                        onClick = onClose,
                        size = 40.dp,
                        tonal = false,
                    )
                }
                if (player != null) {
                    val state by player.state.collectAsState()
                    PlayerPill(
                        modifier = Modifier.align(alignment = Alignment.Center),
                        contentPadding = PaddingValues(all = 4.dp),
                    ) {
                        if (!minimal) PlayerIconButton(
                            icon = Icons.Default.Replay10,
                            description = stringResource(resource = Res.string.player_back),
                            onClick = { dispatchAction { player.skip(deltaMillis = -10_000L) } },
                            enabled = !failed,
                            size = 40.dp,
                            shortcut = "J",
                            tonal = false,
                        )
                        PlayerPlayButton(
                            paused = !state.playWhenReady,
                            buffering = state.isLoadingOrBuffering,
                            enabled = !failed,
                            onClick = { dispatchAction { player.togglePlayWhenReady() } },
                            size = 48.dp,
                        )
                        if (!minimal) PlayerIconButton(
                            icon = Icons.Default.Forward10,
                            description = stringResource(resource = Res.string.player_forward),
                            onClick = { dispatchAction { player.skip(deltaMillis = 10_000L) } },
                            enabled = !failed,
                            size = 40.dp,
                            shortcut = "L",
                            tonal = false,
                        )
                    }
                    if (!minimal) PipTime(
                        player = player,
                        modifier = Modifier
                            .align(alignment = Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 12.dp),
                    )
                }
            }
        }
        if (player != null) PipProgressEdge(
            player = player,
            modifier = Modifier.align(alignment = Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PipTime(
    player: EmbeddedPlayer,
    modifier: Modifier = Modifier,
) {
    val position by player.currentPositionMillis.collectAsState()
    val duration = player.mediaProperties.collectAsState().value?.durationMillis ?: 0L
    PlayerPill(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = "${playerTime(milliseconds = position)} / ${playerTime(milliseconds = duration)}",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Always-visible progress line along the bottom edge of the mini window. Decorative: the dock has the slider. */
@Composable
private fun PipProgressEdge(
    player: EmbeddedPlayer,
    modifier: Modifier = Modifier,
) {
    val position by player.currentPositionMillis.collectAsState()
    val duration = player.mediaProperties.collectAsState().value?.durationMillis ?: 0L
    LinearProgressIndicator(
        progress = { if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
        modifier = modifier
            .fillMaxWidth()
            .height(height = 4.dp),
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** [PlayerVideoTheme] that keeps the surrounding [BoxScope], so children can still align themselves. */
@Composable
private fun BoxScope.VideoThemeBox(
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = this
    PlayerVideoTheme { scope.content() }
}
