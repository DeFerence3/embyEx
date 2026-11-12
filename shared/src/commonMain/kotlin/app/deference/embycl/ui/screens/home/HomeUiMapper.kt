package app.deference.embycl.ui.screens.home

import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionResult
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import app.deference.embycl.ui.screens.home.model.HomeContentUi
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.home.model.HomeSectionUi
import app.deference.embycl.ui.screens.home.model.HomeSpotlightKind
import app.deference.embycl.ui.screens.home.model.HomeSpotlightUi

private const val TICKS_PER_MINUTE = 600_000_000L
private const val JUST_ADDED_LIMIT = 12

internal fun EmbyHome.toContentUi(
    layout: HomeLayoutPrefs,
    previous: HomeSpotlightUi?,
): HomeContentUi {
    val resolved = layout.resolvedFor(libraries = libraries)
    val names = libraries.associate { it.id to it.name }
    val topOn = resolved.top == HomeTop.ContinueWatching
    val inProgress = (sections[HomeSectionId.ContinueWatching] as? HomeSectionResult.Loaded)?.items.orEmpty()
    val newest = resolved.visibleSections
        .filterIsInstance<HomeSectionId.LatestIn>()
        .flatMap { id -> (sections[id] as? HomeSectionResult.Loaded)?.items.orEmpty() }
        .filterNot { it.isFolder }
    val spotlight = when {
        !topOn -> null
        inProgress.isNotEmpty() -> spotlightOf(
            kind = HomeSpotlightKind.ContinueWatching,
            items = inProgress.map { it.toCardUi(inContinueWatching = true) },
            previous = previous,
        )
        newest.isNotEmpty() -> spotlightOf(
            kind = HomeSpotlightKind.JustAdded,
            items = newest.take(JUST_ADDED_LIMIT).map { it.toCardUi(inContinueWatching = false) },
            previous = previous,
        )
        else -> null
    }
    val shelves = resolved.visibleSections.mapNotNull { id ->
        if (id == HomeSectionId.ContinueWatching && topOn) return@mapNotNull null
        val result = sections[id] ?: return@mapNotNull null
        val kind = when (id) {
            HomeSectionId.ContinueWatching -> HomeSectionKind.ContinueWatching
            is HomeSectionId.LatestIn -> HomeSectionKind.LatestIn(libraryName = names[id.libraryId] ?: return@mapNotNull null)
            HomeSectionId.Favorites -> HomeSectionKind.Favorites
            HomeSectionId.Downloaded -> HomeSectionKind.Downloaded
        }
        when (result) {
            is HomeSectionResult.Loaded -> result.items.takeIf { it.isNotEmpty() }?.let { items ->
                HomeSectionUi(
                    key = id.key,
                    kind = kind,
                    cards = items.map { it.toCardUi(inContinueWatching = id == HomeSectionId.ContinueWatching) },
                    total = result.total,
                    failed = false,
                )
            }
            HomeSectionResult.Failed -> HomeSectionUi(
                key = id.key,
                kind = kind,
                cards = emptyList(),
                total = null,
                failed = true,
            )
        }
    }
    return HomeContentUi(
        spotlight = spotlight,
        sections = shelves,
    )
}

private fun spotlightOf(
    kind: HomeSpotlightKind,
    items: List<HomeCardUi>,
    previous: HomeSpotlightUi?,
): HomeSpotlightUi {
    val keep = previous
        ?.takeIf { it.kind == kind }
        ?.let { old -> items.indexOfFirst { it.id == old.current.id } }
        ?.takeIf { it >= 0 }
    return HomeSpotlightUi(
        kind = kind,
        items = items,
        selected = keep ?: 0,
    )
}

internal fun EmbyItem.toCardUi(inContinueWatching: Boolean): HomeCardUi {
    val data = userData
    val position = data?.playbackPositionTicks ?: 0L
    val runtime = runTimeTicks
    val inProgress = position > 0 && data?.played != true
    val still = if (isEpisode()) imageUrl(type = "Primary") ?: imageUrl(type = "Backdrop") else imageUrl(type = "Backdrop") ?: imageUrl(type = "Primary")
    return HomeCardUi(
        id = id,
        title = name,
        type = type,
        isFolder = isFolder,
        kind = when {
            inProgress -> HomeCardKind.Resume
            inContinueWatching && isEpisode() -> HomeCardKind.NextUp
            else -> HomeCardKind.Unstarted
        },
        seriesId = seriesId,
        seriesName = seriesName,
        episodeLabel = episodeLabel(),
        year = productionYear,
        minutesLeft = if (inProgress && runtime != null) ((runtime - position) / TICKS_PER_MINUTE).toInt().coerceAtLeast(1) else null,
        runtimeMinutes = runtime?.let { (it / TICKS_PER_MINUTE).toInt() }?.takeIf { it > 0 },
        progress = if (inProgress) data?.playedPercentage?.let { (it / 100.0).toFloat().coerceIn(0f, 1f) } else null,
        isWatched = data?.played == true,
        isFavorite = data?.isFavorite == true,
        isDownloaded = isAvailableOffline,
        posterUrl = imageUrl(type = "Primary"),
        stillUrl = still,
        backdropUrl = imageUrl(type = "Backdrop", maxWidth = 1600) ?: still,
    )
}

internal fun HomeContentUi.updateCards(
    itemId: String,
    transform: (HomeCardUi) -> HomeCardUi,
): HomeContentUi {
    fun List<HomeCardUi>.apply() = map { if (it.id == itemId) transform(it) else it }
    return copy(
        spotlight = spotlight?.copy(items = spotlight.items.apply()),
        sections = sections.map { it.copy(cards = it.cards.apply()) },
    )
}

internal fun HomeContentUi.withoutInProgress(itemId: String): HomeContentUi {
    val shelves = sections.mapNotNull { section ->
        if (section.kind != HomeSectionKind.ContinueWatching) return@mapNotNull section
        val cards = section.cards.filterNot { it.id == itemId }
        if (cards.isEmpty()) null else section.copy(cards = cards, total = section.total?.minus(1))
    }
    val current = spotlight?.takeIf { it.kind == HomeSpotlightKind.ContinueWatching } ?: return copy(sections = shelves)
    val items = current.items.filterNot { it.id == itemId }
    return copy(
        spotlight = if (items.isEmpty()) null else current.copy(items = items, selected = current.selected.coerceAtMost(items.lastIndex)),
        sections = shelves,
    )
}

internal fun EmbyHome.withSection(
    id: HomeSectionId,
    result: HomeSectionResult,
): EmbyHome = copy(sections = sections + (id to result))

internal fun HomeContentUi.findCard(itemId: String): HomeCardUi? =
    spotlight?.items?.firstOrNull { it.id == itemId }
        ?: sections.firstNotNullOfOrNull { section -> section.cards.firstOrNull { it.id == itemId } }
