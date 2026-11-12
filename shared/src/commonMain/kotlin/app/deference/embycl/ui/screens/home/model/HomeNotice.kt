package app.deference.embycl.ui.screens.home.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface HomeNotice {
    data class WatchedChanged(
        val itemId: String,
        val title: String,
        val watched: Boolean,
    ) : HomeNotice

    data class FavoriteChanged(
        val title: String,
        val added: Boolean,
    ) : HomeNotice

    data class RemovedFromContinue(
        val itemId: String,
        val title: String,
    ) : HomeNotice

    data object DownloadQueued : HomeNotice

    data class Message(val text: String) : HomeNotice

    data class ActionFailed(val title: String) : HomeNotice

    data object RefreshFailed : HomeNotice
}
