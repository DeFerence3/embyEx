package app.deference.embycl.domain.model.home

import app.deference.embycl.domain.model.EmbyItem

data class HomeLayoutPrefs(
    val top: HomeTop = HomeTop.ContinueWatching,
    val cardSize: HomeCardSize = HomeCardSize.Comfortable,
    val sections: List<HomeSectionPref> = emptyList(),
) {
    val visibleSections: List<HomeSectionId> get() = sections.filter { it.visible }.map { it.id }

    fun resolvedFor(
        libraries: List<EmbyItem>,
        keepMissingLibraries: Boolean = false,
    ): HomeLayoutPrefs {
        val latest = libraries.map { library ->
            HomeSectionPref(
                id = HomeSectionId.LatestIn(libraryId = library.id),
                visible = library.collectionType in SHOWN_BY_DEFAULT,
            )
        }

        return if (sections.isEmpty() && !keepMissingLibraries) {
            copy(sections = listOf(visible(id = HomeSectionId.ContinueWatching)) + latest + FIXED_TAIL.map { visible(id = it) })
        } else {
            val libraryIds = libraries.map { it.id }.toSet()
            val kept = sections
                .distinctBy { it.id }
                .filter { pref -> keepMissingLibraries || (pref.id as? HomeSectionId.LatestIn)?.let { it.libraryId in libraryIds } ?: true }
            val present = kept.map { it.id }.toSet()
            val head = if (HomeSectionId.ContinueWatching in present) emptyList() else listOf(visible(id = HomeSectionId.ContinueWatching))
            val newLibraries = latest.filter { it.id !in present }
            val missingTail = FIXED_TAIL.filter { it !in present }.map { visible(id = it) }
            copy(sections = head + kept + newLibraries + missingTail)
        }
    }

    private fun visible(id: HomeSectionId) = HomeSectionPref(
        id = id,
        visible = true,
    )

    companion object {
        private val SHOWN_BY_DEFAULT = setOf("movies", "tvshows")
        private val FIXED_TAIL = listOf(HomeSectionId.Favorites, HomeSectionId.Downloaded)
    }
}
