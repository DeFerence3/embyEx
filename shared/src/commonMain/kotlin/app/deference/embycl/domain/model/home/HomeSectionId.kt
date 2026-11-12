package app.deference.embycl.domain.model.home

sealed interface HomeSectionId {
    val key: String

    data object ContinueWatching : HomeSectionId {
        override val key: String = "continue"
    }

    data class LatestIn(val libraryId: String) : HomeSectionId {
        override val key: String = "$LATEST_PREFIX$libraryId"
    }

    data object Favorites : HomeSectionId {
        override val key: String = "favorites"
    }

    data object Downloaded : HomeSectionId {
        override val key: String = "downloaded"
    }

    companion object {
        private const val LATEST_PREFIX = "latest:"

        fun fromKey(key: String): HomeSectionId? = when {
            key == ContinueWatching.key -> ContinueWatching
            key == Favorites.key -> Favorites
            key == Downloaded.key -> Downloaded
            key.startsWith(LATEST_PREFIX) && key.length > LATEST_PREFIX.length -> LatestIn(libraryId = key.removePrefix(LATEST_PREFIX))
            else -> null
        }
    }
}
