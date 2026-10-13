package app.deference.embycl.ui.screens.downloads

sealed interface DownloadsAction {
    data class QueryChanged(val query: String) : DownloadsAction
    data class FilterChanged(val filter: DownloadFilter) : DownloadsAction
    data class Play(val id: String) : DownloadsAction
    data class Pause(val id: String) : DownloadsAction
    data class Resume(val id: String) : DownloadsAction
    data class Retry(val id: String) : DownloadsAction
    data class Cancel(val id: String) : DownloadsAction
    data class Remove(val id: String) : DownloadsAction
    data object BrowseLibrary : DownloadsAction
    data object Reconnect : DownloadsAction
    data object SignIn : DownloadsAction
    data object Reload : DownloadsAction
}
