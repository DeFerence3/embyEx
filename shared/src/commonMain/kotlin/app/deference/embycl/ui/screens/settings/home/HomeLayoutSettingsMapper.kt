package app.deference.embycl.ui.screens.settings.home

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionPref
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.settings.home.model.HomeLayoutRowUi

/** Rows for a resolved layout; "New in" rows for libraries not in [libraries] (unknown name) are left out. */
internal fun HomeLayoutPrefs.toLayoutRows(libraries: List<EmbyItem>): List<HomeLayoutRowUi> {
    val names = libraries.associate { it.id to it.name }
    val pinnedFirst = top == HomeTop.ContinueWatching && sections.firstOrNull()?.id == HomeSectionId.ContinueWatching
    val floor = if (pinnedFirst) 1 else 0
    return sections.mapIndexedNotNull { index, pref ->
        val kind = when (val id = pref.id) {
            HomeSectionId.ContinueWatching -> HomeSectionKind.ContinueWatching
            is HomeSectionId.LatestIn -> HomeSectionKind.LatestIn(libraryName = names[id.libraryId] ?: return@mapIndexedNotNull null)
            HomeSectionId.Favorites -> HomeSectionKind.Favorites
            HomeSectionId.Downloaded -> HomeSectionKind.Downloaded
        }
        val pinned = pinnedFirst && index == 0
        HomeLayoutRowUi(
            key = pref.id.key,
            kind = kind,
            visible = pref.visible,
            pinned = pinned,
            canMoveUp = !pinned && index - 1 >= floor,
            canMoveDown = !pinned && index < sections.lastIndex,
        )
    }
}

/** Moves one section a step; Continue watching stays first while it is pinned to the top. */
internal fun List<HomeSectionPref>.moved(
    key: String,
    up: Boolean,
    pinnedFirst: Boolean,
): List<HomeSectionPref> {
    val index = indexOfFirst { it.id.key == key }
    if (index < 0) return this
    if (pinnedFirst && this[index].id == HomeSectionId.ContinueWatching) return this
    val target = if (up) index - 1 else index + 1
    val floor = if (pinnedFirst && firstOrNull()?.id == HomeSectionId.ContinueWatching) 1 else 0
    if (target !in floor..lastIndex) return this
    return toMutableList().apply {
        val row = removeAt(index)
        add(target, row)
    }
}
