package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DetailsNotice {
    data object MarkedWatched : DetailsNotice

    data object FavoriteFailed : DetailsNotice

    data object WatchedFailed : DetailsNotice

    data object DownloadQueued : DetailsNotice

    data class Message(val text: String) : DetailsNotice
}
