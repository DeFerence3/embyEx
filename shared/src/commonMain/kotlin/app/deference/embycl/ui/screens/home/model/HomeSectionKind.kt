package app.deference.embycl.ui.screens.home.model

sealed interface HomeSectionKind {
    data object ContinueWatching : HomeSectionKind
    data class LatestIn(val libraryName: String) : HomeSectionKind
    data object Favorites : HomeSectionKind
    data object Downloaded : HomeSectionKind
}
