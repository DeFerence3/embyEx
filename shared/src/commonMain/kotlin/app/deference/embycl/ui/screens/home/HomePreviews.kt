package app.deference.embycl.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.theme.EmbympvTheme
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.adaptive.PreviewDeviceConfiguration
import app.deference.embycl.ui.screens.home.components.HomeFilmstrip
import app.deference.embycl.ui.screens.home.components.HomeFullStateView
import app.deference.embycl.ui.screens.home.components.HomeLayout
import app.deference.embycl.ui.screens.home.components.HomePosterCard
import app.deference.embycl.ui.screens.home.components.HomeSkeleton
import app.deference.embycl.ui.screens.home.components.HomeSourceChip
import app.deference.embycl.ui.screens.home.components.HomeSpotlight
import app.deference.embycl.ui.screens.home.components.HomeStatusCallout
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeContentUi
import app.deference.embycl.ui.screens.home.model.HomeFullState
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.home.model.HomeSectionUi
import app.deference.embycl.ui.screens.home.model.HomeSourceUi
import app.deference.embycl.ui.screens.home.model.HomeSpotlightKind
import app.deference.embycl.ui.screens.home.model.HomeSpotlightUi

private fun previewCard(
    id: String,
    title: String,
    seriesName: String? = null,
    episodeLabel: String? = null,
    year: Int? = null,
    minutesLeft: Int? = null,
    progress: Float? = null,
    watched: Boolean = false,
    downloaded: Boolean = false,
    nextUp: Boolean = false,
) = HomeCardUi(
    id = id,
    title = title,
    type = if (seriesName != null) "Episode" else "Movie",
    isFolder = false,
    kind = when {
        progress != null -> HomeCardKind.Resume
        nextUp -> HomeCardKind.NextUp
        else -> HomeCardKind.Unstarted
    },
    seriesId = seriesName?.let { "series-$id" },
    seriesName = seriesName,
    episodeLabel = episodeLabel,
    year = year,
    minutesLeft = minutesLeft,
    runtimeMinutes = 52,
    progress = progress,
    isWatched = watched,
    isFavorite = false,
    isDownloaded = downloaded,
    posterUrl = null,
    stillUrl = null,
    backdropUrl = null,
)

private val inProgress = listOf(
    previewCard(id = "1", title = "The Long Way Round", seriesName = "Northbound", episodeLabel = "S2 · E5", minutesLeft = 23, progress = 0.56f),
    previewCard(id = "2", title = "Salt and Signal", year = 2023, minutesLeft = 72, progress = 0.31f),
    previewCard(id = "5", title = "Ash on the Wind", seriesName = "Copper Valley", episodeLabel = "S1 · E4", nextUp = true),
    previewCard(id = "3", title = "Deadline", seriesName = "The Night Desk", episodeLabel = "S3 · E1", minutesLeft = 9, progress = 0.82f),
    previewCard(id = "4", title = "Paper Lanterns", year = 2021, minutesLeft = 38, progress = 0.64f),
)

private val recent = listOf(
    previewCard(id = "10", title = "Low Tide", year = 2024, downloaded = true),
    previewCard(id = "11", title = "The Glass Orchard", year = 2024),
    previewCard(id = "12", title = "Kestrel", year = 2023, watched = true),
    previewCard(id = "13", title = "A Map of Small Hours: The Director’s Extended Cut", year = 2024),
)

private val spotlight = HomeSpotlightUi(
    kind = HomeSpotlightKind.ContinueWatching,
    items = inProgress,
    selected = 0,
)

private val loadedState = HomeState(
    content = HomeContentUi(
        spotlight = spotlight,
        sections = listOf(
            HomeSectionUi(
                key = "latest:movies",
                kind = HomeSectionKind.LatestIn(libraryName = "Movies"),
                cards = recent,
                total = null,
                failed = false,
            ),
            HomeSectionUi(
                key = "latest:shows",
                kind = HomeSectionKind.LatestIn(libraryName = "TV Shows"),
                cards = emptyList(),
                total = null,
                failed = true,
            ),
            HomeSectionUi(
                key = "downloaded",
                kind = HomeSectionKind.Downloaded,
                cards = inProgress.take(2).map { it.copy(isDownloaded = true) },
                total = 2,
                failed = false,
            ),
        ),
    ),
    source = HomeSourceUi.Connected,
    serverName = "Harbour",
)

@Composable
private fun PreviewSurface(
    dark: Boolean = false,
    configuration: DeviceConfiguration = DeviceConfiguration.MOBILE_PORTRAIT,
    content: @Composable () -> Unit,
) {
    EmbympvTheme(
        darkTheme = dark,
        dynamicColor = false,
    ) {
        PreviewDeviceConfiguration(configuration = configuration) {
            Column(modifier = Modifier.background(color = MaterialTheme.colorScheme.background)) { content() }
        }
    }
}

@Composable
private fun HomeContentPreview(
    state: HomeState,
    dark: Boolean = false,
    configuration: DeviceConfiguration = DeviceConfiguration.MOBILE_PORTRAIT,
) {
    PreviewSurface(
        dark = dark,
        configuration = configuration,
    ) {
        HomeContent(
            state = state,
            onAction = {},
            onConfirmDownload = {},
            onEditHome = {},
        )
    }
}

// ---------- Whole screen, per window configuration and state

@Preview(name = "Home · phone · light", widthDp = 393, heightDp = 873)
@Composable
private fun HomePhonePreview() = HomeContentPreview(state = loadedState)

@Preview(name = "Home · phone · dark", widthDp = 393, heightDp = 873)
@Composable
private fun HomePhoneDarkPreview() = HomeContentPreview(
    state = loadedState,
    dark = true,
)

@Preview(name = "Home · short window", widthDp = 873, heightDp = 393)
@Composable
private fun HomeShortPreview() = HomeContentPreview(
    state = loadedState,
    configuration = DeviceConfiguration.MOBILE_LANDSCAPE,
)

@Preview(name = "Home · tablet", widthDp = 800, heightDp = 1280)
@Composable
private fun HomeMediumPreview() = HomeContentPreview(
    state = loadedState,
    configuration = DeviceConfiguration.TABLET_PORTRAIT,
)

@Preview(name = "Home · wide · dark", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeWideDarkPreview() = HomeContentPreview(
    state = loadedState,
    dark = true,
    configuration = DeviceConfiguration.TABLET_LANDSCAPE,
)

@Preview(name = "Home · large cards", widthDp = 393, heightDp = 873)
@Composable
private fun HomeLargeCardsPreview() = HomeContentPreview(
    state = loadedState.copy(cardSize = HomeCardSize.Large),
)

@Preview(name = "Full state · sign in again")
@Composable
private fun SignedOutPreview() = FullStatePreview(state = HomeFullState.SignedOut)

@Preview(name = "Home · loading", widthDp = 393, heightDp = 873)
@Composable
private fun HomeLoadingPreview() = HomeContentPreview(state = HomeState())

@Preview(name = "Home · offline with downloads", widthDp = 393, heightDp = 873)
@Composable
private fun HomeOfflinePreview() = HomeContentPreview(
    state = loadedState.copy(source = HomeSourceUi.Offline),
)

@Preview(name = "Home · this device", widthDp = 393, heightDp = 873)
@Composable
private fun HomeDevicePreview() = HomeContentPreview(
    state = loadedState.copy(source = HomeSourceUi.ThisDevice),
)

@Preview(name = "Home · nothing in progress", widthDp = 393, heightDp = 873)
@Composable
private fun HomeJustAddedPreview() = HomeContentPreview(
    state = loadedState.copy(
        content = loadedState.content?.copy(
            spotlight = HomeSpotlightUi(
                kind = HomeSpotlightKind.JustAdded,
                items = recent,
                selected = 0,
            ),
        ),
    ),
)

@Preview(name = "Home · one title in progress, long title", widthDp = 393, heightDp = 873)
@Composable
private fun HomeSinglePreview() = HomeContentPreview(
    state = loadedState.copy(
        content = loadedState.content?.copy(
            spotlight = HomeSpotlightUi(
                kind = HomeSpotlightKind.ContinueWatching,
                items = listOf(inProgress.first().copy(title = "The Extraordinarily Long and Winding Way Round to the Northern Lighthouse")),
                selected = 0,
            ),
        ),
    ),
)

@Preview(name = "Home · text 200 %", widthDp = 393, heightDp = 873)
@Composable
private fun HomeLargeTextPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 2f,
        ),
    ) {
        HomeContentPreview(state = loadedState)
    }
}

// ---------- Full-screen states

@Preview(name = "Full state · offline, nothing downloaded")
@Composable
private fun OfflineEmptyPreview() = FullStatePreview(state = HomeFullState.OfflineEmpty)

@Preview(name = "Full state · server not responding · dark")
@Composable
private fun UnreachablePreview() = FullStatePreview(
    state = HomeFullState.Unreachable,
    dark = true,
)

@Preview(name = "Full state · this device, empty")
@Composable
private fun DeviceEmptyPreview() = FullStatePreview(state = HomeFullState.DeviceEmpty)

@Preview(name = "Full state · server library empty")
@Composable
private fun ServerEmptyPreview() = FullStatePreview(state = HomeFullState.ServerEmpty)

@Composable
private fun FullStatePreview(
    state: HomeFullState,
    dark: Boolean = false,
) {
    PreviewSurface(dark = dark) {
        HomeFullStateView(
            state = state,
            serverName = "Harbour",
            onTryAgain = {},
            onSignIn = {},
        )
    }
}

// ---------- Pieces

@Preview(name = "Spotlight · phone · dark")
@Composable
private fun SpotlightPhoneDarkPreview() {
    PreviewSurface(dark = true) {
        HomeSpotlight(
            spotlight = spotlight,
            layout = HomeLayout.Phone,
            onPlay = {},
            onOpenDetails = {},
            onOpenMenu = {},
        )
    }
}

@Preview(name = "Spotlight · wide", widthDp = 1280)
@Composable
private fun SpotlightWidePreview() {
    PreviewSurface {
        HomeSpotlight(
            spotlight = spotlight.copy(selected = 1),
            layout = HomeLayout.Wide,
            onPlay = {},
            onOpenDetails = {},
            onOpenMenu = {},
        )
    }
}

@Preview(name = "Filmstrip · medium, third selected", widthDp = 800)
@Composable
private fun FilmstripPreview() {
    PreviewSurface {
        HomeFilmstrip(
            spotlight = spotlight.copy(selected = 2),
            layout = HomeLayout.Medium,
            onSelect = {},
        )
    }
}

@Preview(name = "Poster cards · default, downloaded, watched, long title")
@Composable
private fun PosterCardsPreview() {
    PreviewSurface {
        Row(
            modifier = Modifier.padding(all = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            recent.forEach { card ->
                HomePosterCard(
                    card = card,
                    width = 144.dp,
                    onOpen = {},
                    onOpenMenu = {},
                )
            }
        }
    }
}

@Preview(name = "Source chip · every status")
@Composable
private fun SourceChipsPreview() {
    PreviewSurface {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            HomeSourceUi.entries.forEach { source ->
                HomeSourceChip(
                    source = source,
                    serverName = "Harbour",
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "Callouts · offline, this device · dark")
@Composable
private fun CalloutsDarkPreview() {
    PreviewSurface(dark = true) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            HomeStatusCallout(
                source = HomeSourceUi.Offline,
                serverName = "Harbour",
                onTryAgain = {},
                onSignIn = {},
            )
            HomeStatusCallout(
                source = HomeSourceUi.ThisDevice,
                serverName = "Harbour",
                onTryAgain = {},
                onSignIn = {},
            )
            HomeStatusCallout(
                source = HomeSourceUi.SignedOut,
                serverName = "Harbour",
                onTryAgain = {},
                onSignIn = {},
            )
        }
    }
}

@Preview(name = "Skeleton · wide", widthDp = 1280)
@Composable
private fun SkeletonWidePreview() {
    PreviewSurface {
        HomeSkeleton(layout = HomeLayout.Wide)
    }
}
