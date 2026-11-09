package app.deference.embycl.ui.screens.details

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.download.DownloadSource
import app.deference.embycl.ui.screens.details.components.DetailsActions
import app.deference.embycl.ui.screens.details.components.DetailsEpisodeNav
import app.deference.embycl.ui.screens.details.components.DetailsHero
import app.deference.embycl.ui.screens.details.components.DetailsIdentity
import app.deference.embycl.ui.screens.details.components.DetailsLayout
import app.deference.embycl.ui.screens.details.components.DetailsLoadError
import app.deference.embycl.ui.screens.details.components.DetailsLoading
import app.deference.embycl.ui.screens.details.components.DetailsMediaInfo
import app.deference.embycl.ui.screens.details.components.DetailsOfflineBanner
import app.deference.embycl.ui.screens.details.components.DetailsOverview
import app.deference.embycl.ui.screens.details.components.DetailsPeopleRail
import app.deference.embycl.ui.screens.details.components.DetailsPlayBlock
import app.deference.embycl.ui.screens.details.components.DetailsSeasonRail
import app.deference.embycl.ui.screens.details.components.DetailsTopBar
import app.deference.embycl.ui.screens.details.components.DetailsTrackChips
import app.deference.embycl.ui.screens.details.components.RemoveDownloadDialog
import app.deference.embycl.ui.screens.details.model.DetailsNotice
import app.deference.embycl.ui.screens.details.model.DetailsUi
import app.deference.embycl.ui.screens.details.model.SeasonUi
import app.deference.embycl.ui.screens.downloads.components.DownloadSourceDialog
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_notice_favorite_failed
import embyex.shared.generated.resources.details_notice_marked_watched
import embyex.shared.generated.resources.details_notice_queued
import embyex.shared.generated.resources.details_notice_undo
import embyex.shared.generated.resources.details_notice_watched_failed
import org.jetbrains.compose.resources.getString

@Composable
internal fun EmbyDetailsScreenContent(
    state: EmbyDetailsState,
    onAction: (EmbyDetailsAction) -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayFromServer: () -> Unit,
    onConfirmDownload: (DownloadSource) -> Unit,
    onOpenSeries: (seriesId: String, seriesName: String) -> Unit,
    onOpenEpisode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val currentOnAction by rememberUpdatedState(newValue = onAction)
    LaunchedEffect(key1 = state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = notice.message(),
            actionLabel = if (notice == DetailsNotice.MarkedWatched) getString(resource = Res.string.details_notice_undo) else null,
            duration = if (notice == DetailsNotice.MarkedWatched) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed && notice == DetailsNotice.MarkedWatched) {
            currentOnAction(EmbyDetailsAction.UndoWatched)
        }
        currentOnAction(EmbyDetailsAction.DismissNotice)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val scale = DetailsLayout.scaleFor(
                width = maxWidth,
                height = maxHeight,
            )
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * scale,
                    fontScale = density.fontScale,
                ),
            ) {
                ScaledDetails(
                    state = state,
                    scrollState = scrollState,
                    snackbar = snackbar,
                    onAction = onAction,
                    onBack = onBack,
                    onPlay = onPlay,
                    onPlayFromServer = onPlayFromServer,
                    onOpenSeries = onOpenSeries,
                    onOpenEpisode = onOpenEpisode,
                )
            }
        }
    }

    if (state.confirmRemove) {
        RemoveDownloadDialog(
            title = state.content?.getOrNull()?.title.orEmpty(),
            onConfirm = { onAction(EmbyDetailsAction.ConfirmRemoveDownload) },
            onDismiss = { onAction(EmbyDetailsAction.DismissRemoveDownload) },
        )
    }
    state.choice?.let { choice ->
        DownloadSourceDialog(
            title = choice.title,
            sources = choice.sources,
            selectedKey = choice.selected,
            serverAvailable = !state.isOffline,
            onSelect = { key -> onAction(EmbyDetailsAction.SelectSource(key = key)) },
            onConfirm = onConfirmDownload,
            onDismiss = { onAction(EmbyDetailsAction.DismissDownload) },
            isSubmitting = choice.submitting,
        )
    }
}

@Composable
private fun ScaledDetails(
    state: EmbyDetailsState,
    scrollState: ScrollState,
    snackbar: SnackbarHostState,
    onAction: (EmbyDetailsAction) -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPlayFromServer: () -> Unit,
    onOpenSeries: (seriesId: String, seriesName: String) -> Unit,
    onOpenEpisode: (String) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Escape) {
                    onBack()
                    true
                } else {
                    false
                }
            },
    ) {
        val layout = DetailsLayout.fromSize(
            width = maxWidth,
            height = maxHeight,
        )
        val content = state.content
        val details = content?.getOrNull()
        val threshold = with(LocalDensity.current) { (maxHeight * 0.5f).roundToPx() }
        val collapsed by remember(threshold) { derivedStateOf { scrollState.value > threshold } }
        when {
            content == null -> DetailsLoading(layout = layout)
            details == null -> DetailsLoadError(onRetry = { onAction(EmbyDetailsAction.Retry) })
            else -> LoadedDetails(
                details = details,
                state = state,
                layout = layout,
                viewportHeight = maxHeight,
                scrollState = scrollState,
                onAction = onAction,
                onPlay = onPlay,
                onPlayFromServer = onPlayFromServer,
                onOpenSeries = onOpenSeries,
                onOpenEpisode = onOpenEpisode,
            )
        }
        DetailsTopBar(
            title = details?.title,
            collapsed = collapsed,
            onBack = onBack,
        )
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(all = 16.dp),
        )
    }
}

private suspend fun DetailsNotice.message(): String = when (this) {
    DetailsNotice.MarkedWatched -> getString(resource = Res.string.details_notice_marked_watched)
    DetailsNotice.FavoriteFailed -> getString(resource = Res.string.details_notice_favorite_failed)
    DetailsNotice.WatchedFailed -> getString(resource = Res.string.details_notice_watched_failed)
    DetailsNotice.DownloadQueued -> getString(resource = Res.string.details_notice_queued)
    is DetailsNotice.Message -> text
}

@Composable
private fun LoadedDetails(
    details: DetailsUi,
    state: EmbyDetailsState,
    layout: DetailsLayout,
    viewportHeight: Dp,
    scrollState: ScrollState,
    onAction: (EmbyDetailsAction) -> Unit,
    onPlay: () -> Unit,
    onPlayFromServer: () -> Unit,
    onOpenSeries: (seriesId: String, seriesName: String) -> Unit,
    onOpenEpisode: (String) -> Unit,
) {
    val compact = layout == DetailsLayout.Compact
    val side: Dp = when (layout) {
        DetailsLayout.Compact -> 16.dp
        DetailsLayout.Medium -> 32.dp
        DetailsLayout.Expanded -> 48.dp
    }
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(
        key1 = details.id,
        key2 = layout,
    ) {
        if (layout == DetailsLayout.Expanded) {
            try {
                playFocus.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
    }
    val season = state.season
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState),
    ) {
        DetailsHero(
            backdropUrl = details.backdropUrl,
            layout = layout,
            viewportHeight = viewportHeight,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
                DetailsIdentity(
                    title = details.title,
                    logoUrl = details.logoUrl,
                    episode = details.episode,
                    year = details.year,
                    runtime = details.runtime,
                    officialRating = details.officialRating,
                    communityRating = details.communityRating,
                    videoLabel = details.videoLabel,
                    genres = details.genres,
                    onOpenSeries = onOpenSeries,
                )
                if (state.isOffline) DetailsOfflineBanner()
                DetailsPlayBlock(
                    playback = details.playback,
                    fillWidth = compact,
                    onPlay = onPlay,
                    onStartOver = null,
                    focusRequester = playFocus,
                )
                DetailsActions(
                    isFavorite = details.isFavorite,
                    isPlayed = details.isPlayed,
                    download = state.download,
                    togglesEnabled = !state.isOffline,
                    fillWidth = compact,
                    onToggleFavorite = { onAction(EmbyDetailsAction.ToggleFavorite) },
                    onToggleWatched = { onAction(EmbyDetailsAction.ToggleWatched) },
                    onDownload = { onAction(EmbyDetailsAction.Download) },
                    onPlayFromServer = onPlayFromServer,
                    onRemoveDownload = { onAction(EmbyDetailsAction.RequestRemoveDownload) },
                )
                DetailsTrackChips(
                    audioTracks = details.audioTracks,
                    subtitleTracks = details.subtitleTracks,
                    selectedAudioIndex = state.selectedAudioIndex,
                    selectedSubtitleIndex = state.selectedSubtitleIndex,
                    useSheet = compact,
                    onSelectAudio = { index -> onAction(EmbyDetailsAction.SelectAudio(index = index)) },
                    onSelectSubtitle = { index -> onAction(EmbyDetailsAction.SelectSubtitle(index = index)) },
                )
                if (season is SeasonUi.Loaded) {
                    DetailsEpisodeNav(
                        previous = season.previous,
                        next = season.next,
                        onOpenEpisode = onOpenEpisode,
                    )
                }
                if (layout == DetailsLayout.Expanded) {
                    details.overview?.let { text -> DetailsOverview(text = text) }
                }
            }
        }
        Spacer(modifier = Modifier.height(height = 24.dp))
        when (layout) {
            DetailsLayout.Compact -> Column(verticalArrangement = Arrangement.spacedBy(space = 32.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = side),
                    verticalArrangement = Arrangement.spacedBy(space = 32.dp),
                ) {
                    details.overview?.let { text -> DetailsOverview(text = text) }
                    DetailsMediaInfo(facts = details.facts)
                }
                Rails(
                    details = details,
                    season = season,
                    horizontalPadding = side,
                    showScrollButtons = layout != DetailsLayout.Compact,
                    onAction = onAction,
                    onOpenEpisode = onOpenEpisode,
                )
            }
            DetailsLayout.Medium -> Column(verticalArrangement = Arrangement.spacedBy(space = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = side),
                    horizontalArrangement = Arrangement.spacedBy(space = 32.dp),
                ) {
                    Box(modifier = Modifier.weight(weight = 1f)) {
                        details.overview?.let { text -> DetailsOverview(text = text) }
                    }
                    DetailsMediaInfo(
                        facts = details.facts,
                        modifier = Modifier.width(width = 260.dp),
                    )
                }
                Rails(
                    details = details,
                    season = season,
                    horizontalPadding = side,
                    showScrollButtons = layout != DetailsLayout.Compact,
                    onAction = onAction,
                    onOpenEpisode = onOpenEpisode,
                )
            }
            DetailsLayout.Expanded -> Column(verticalArrangement = Arrangement.spacedBy(space = 32.dp)) {
                DetailsMediaInfo(
                    facts = details.facts,
                    horizontal = true,
                    modifier = Modifier.padding(horizontal = side),
                )
                Rails(
                    details = details,
                    season = season,
                    horizontalPadding = side,
                    showScrollButtons = true,
                    onAction = onAction,
                    onOpenEpisode = onOpenEpisode,
                )
            }
        }
        Spacer(modifier = Modifier.height(height = 48.dp))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(insets = WindowInsets.navigationBars))
    }
}

@Composable
private fun Rails(
    details: DetailsUi,
    season: SeasonUi,
    horizontalPadding: Dp,
    showScrollButtons: Boolean,
    onAction: (EmbyDetailsAction) -> Unit,
    onOpenEpisode: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 32.dp)) {
        DetailsSeasonRail(
            season = season,
            horizontalPadding = horizontalPadding,
            showScrollButtons = showScrollButtons,
            onOpenEpisode = onOpenEpisode,
            onRetry = { onAction(EmbyDetailsAction.Retry) },
        )
        DetailsPeopleRail(
            people = details.people,
            horizontalPadding = horizontalPadding,
            showScrollButtons = showScrollButtons,
        )
    }
}
