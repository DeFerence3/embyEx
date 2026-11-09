package app.deference.embycl.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.components.DetailsActions
import app.deference.embycl.ui.screens.details.model.DetailsUi
import app.deference.embycl.ui.screens.details.model.DownloadStatusUi
import app.deference.embycl.ui.screens.details.model.EpisodeContextUi
import app.deference.embycl.ui.screens.details.model.EpisodeLinkUi
import app.deference.embycl.ui.screens.details.model.MediaFactUi
import app.deference.embycl.ui.screens.details.model.PersonKind
import app.deference.embycl.ui.screens.details.model.PersonUi
import app.deference.embycl.ui.screens.details.model.PlaybackUi
import app.deference.embycl.ui.screens.details.model.SeasonEpisodeUi
import app.deference.embycl.ui.screens.details.model.SeasonUi
import app.deference.embycl.ui.screens.details.model.TrackTag
import app.deference.embycl.ui.screens.details.model.TrackUi
import app.deference.embycl.ui.theme.EmbympvTheme
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_fact_audio
import embyex.shared.generated.resources.details_fact_file
import embyex.shared.generated.resources.details_fact_video

private val previewMovie = DetailsUi(
    id = "movie",
    title = "The Long Tide",
    logoUrl = null,
    backdropUrl = null,
    episode = null,
    year = "2024",
    runtime = "2 h 18 m",
    officialRating = "PG-13",
    communityRating = "7.8",
    videoLabel = "4K HEVC HDR10",
    genres = listOf(
        "Drama",
        "Mystery",
        "Thriller",
    ),
    overview = "When a fishing trawler returns to Port Ainsley with no crew aboard, harbour pilot Ines Varga is the only one " +
        "who believes the sea didn't take them. As the tide charts stop matching the calendar, she follows the last radio log " +
        "out past the breakwater, into a storm the village has been hiding for forty years.",
    playback = PlaybackUi.Resume(
        position = "1:12:40",
        progress = 0.52f,
        remainingMinutes = 66,
    ),
    isFavorite = true,
    isPlayed = false,
    audioTracks = listOf(
        TrackUi(
            index = 1,
            name = "English",
            detail = "Dolby Digital+ 5.1",
            tag = null,
            isDefault = true,
        ),
        TrackUi(
            index = 2,
            name = "Spanish",
            detail = "Dolby Digital 5.1",
            tag = null,
            isDefault = false,
        ),
    ),
    subtitleTracks = listOf(
        TrackUi(
            index = 3,
            name = "English",
            detail = null,
            tag = TrackTag.Forced,
            isDefault = false,
        ),
    ),
    people = listOf(
        PersonUi(
            id = "1",
            name = "Mara Ellison",
            role = "Ines Varga",
            kind = PersonKind.Cast,
            photoUrl = null,
            initials = "ME",
        ),
        PersonUi(
            id = "2",
            name = "Tomás Okafor",
            role = "Callum Reid",
            kind = PersonKind.Cast,
            photoUrl = null,
            initials = "TO",
        ),
        PersonUi(
            id = "3",
            name = "Hanne Solberg",
            role = null,
            kind = PersonKind.Director,
            photoUrl = null,
            initials = "HS",
        ),
    ),
    facts = listOf(
        MediaFactUi(
            label = Res.string.details_fact_video,
            value = "2160p · HEVC · HDR10",
        ),
        MediaFactUi(
            label = Res.string.details_fact_audio,
            value = "English, Spanish",
            trackCount = 2,
        ),
        MediaFactUi(
            label = Res.string.details_fact_file,
            value = "MKV · 18.4 GB",
        ),
    ),
    isAvailableOffline = false,
)

private val previewEpisode = previewMovie.copy(
    id = "episode",
    title = "Harbor Lights",
    episode = EpisodeContextUi(
        seriesId = "series",
        seriesName = "Northbound",
        seasonNumber = 2,
        episodeNumber = 5,
    ),
    officialRating = "TV-14",
    playback = PlaybackUi.Start,
    isFavorite = false,
)

private val previewSeason = SeasonUi.Loaded(
    seasonNumber = 2,
    episodes = listOf(
        SeasonEpisodeUi(
            id = "e4",
            title = "Low Water",
            episodeNumber = 4,
            runtime = "52 m",
            stillUrl = null,
            isPlayed = false,
            progress = 0.3f,
            isCurrent = false,
        ),
        SeasonEpisodeUi(
            id = "episode",
            title = "Harbor Lights",
            episodeNumber = 5,
            runtime = "48 m",
            stillUrl = null,
            isPlayed = false,
            progress = null,
            isCurrent = true,
        ),
        SeasonEpisodeUi(
            id = "e6",
            title = "The Breakwater",
            episodeNumber = 6,
            runtime = "51 m",
            stillUrl = null,
            isPlayed = false,
            progress = null,
            isCurrent = false,
        ),
    ),
    previous = EpisodeLinkUi(
        id = "e4",
        title = "Low Water",
        seasonNumber = 2,
        episodeNumber = 4,
    ),
    next = EpisodeLinkUi(
        id = "e6",
        title = "The Breakwater",
        seasonNumber = 2,
        episodeNumber = 6,
    ),
)

@Composable
private fun PreviewScreen(
    state: EmbyDetailsState,
    darkTheme: Boolean = false,
) {
    EmbympvTheme(
        darkTheme = darkTheme,
        dynamicColor = false,
    ) {
        EmbyDetailsScreenContent(
            state = state,
            onAction = {},
            onBack = {},
            onPlay = {},
            onPlayFromServer = {},
            onConfirmDownload = {},
            onOpenSeries = { _, _ -> },
            onOpenEpisode = {},
        )
    }
}

@Preview(name = "Movie · resuming", widthDp = 390, heightDp = 1400)
@Composable
private fun MovieResumingPreview() {
    PreviewScreen(state = EmbyDetailsState(content = Result.success(value = previewMovie)))
}

@Preview(name = "Movie · resuming · dark", widthDp = 390, heightDp = 1400)
@Composable
private fun MovieResumingDarkPreview() {
    PreviewScreen(
        state = EmbyDetailsState(content = Result.success(value = previewMovie)),
        darkTheme = true,
    )
}

@Preview(name = "Movie · tablet", widthDp = 720, heightDp = 1200)
@Composable
private fun MovieTabletPreview() {
    PreviewScreen(state = EmbyDetailsState(content = Result.success(value = previewMovie)))
}

@Preview(name = "Movie · desktop · dark", widthDp = 1280, heightDp = 900)
@Composable
private fun MovieDesktopPreview() {
    PreviewScreen(
        state = EmbyDetailsState(content = Result.success(value = previewMovie)),
        darkTheme = true,
    )
}

@Preview(name = "Episode with season", widthDp = 390, heightDp = 1600)
@Composable
private fun EpisodePreview() {
    PreviewScreen(
        state = EmbyDetailsState(
            content = Result.success(value = previewEpisode),
            season = previewSeason,
        ),
    )
}

@Preview(name = "Offline, downloaded", widthDp = 390, heightDp = 1200)
@Composable
private fun OfflinePreview() {
    PreviewScreen(
        state = EmbyDetailsState(
            content = Result.success(value = previewMovie.copy(isAvailableOffline = true)),
            download = DownloadStatusUi.Downloaded,
            isOffline = true,
        ),
    )
}

@Preview(name = "Loading", widthDp = 390, heightDp = 844)
@Composable
private fun LoadingPreview() {
    PreviewScreen(state = EmbyDetailsState())
}

@Preview(name = "Load failed", widthDp = 390, heightDp = 844)
@Composable
private fun LoadFailedPreview() {
    PreviewScreen(state = EmbyDetailsState(content = Result.failure(exception = IllegalStateException("Server didn't respond"))))
}

@Preview(name = "Long title, sparse", widthDp = 390, heightDp = 1000)
@Composable
private fun SparsePreview() {
    PreviewScreen(
        state = EmbyDetailsState(
            content = Result.success(
                value = previewMovie.copy(
                    title = "The Extraordinary and Unlikely Voyage of the Northern Lighthouse Keeper's Daughter",
                    genres = emptyList(),
                    overview = null,
                    people = emptyList(),
                    communityRating = null,
                    playback = PlaybackUi.Start,
                    audioTracks = previewMovie.audioTracks.take(n = 1),
                    subtitleTracks = emptyList(),
                ),
            ),
        ),
    )
}

@Preview(name = "Font scale 2x", widthDp = 390, heightDp = 2000)
@Composable
private fun LargeFontPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 2f,
        ),
    ) {
        PreviewScreen(state = EmbyDetailsState(content = Result.success(value = previewMovie)))
    }
}

@Preview(name = "Download tile states", widthDp = 420)
@Composable
private fun DownloadStatesPreview() {
    EmbympvTheme(
        darkTheme = false,
        dynamicColor = false,
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            listOf(
                DownloadStatusUi.Idle,
                DownloadStatusUi.Checking,
                DownloadStatusUi.Queued,
                DownloadStatusUi.Downloading(progress = 0.42f),
                DownloadStatusUi.Paused(progress = 0.42f),
                DownloadStatusUi.Downloaded,
                DownloadStatusUi.Failed,
                DownloadStatusUi.Unavailable,
            ).forEach { status ->
                DetailsActions(
                    isFavorite = status == DownloadStatusUi.Downloaded,
                    isPlayed = status == DownloadStatusUi.Failed,
                    download = status,
                    togglesEnabled = status != DownloadStatusUi.Unavailable,
                    fillWidth = true,
                    onToggleFavorite = {},
                    onToggleWatched = {},
                    onDownload = {},
                    onPlayFromServer = {},
                    onRemoveDownload = {},
                )
            }
        }
    }
}
