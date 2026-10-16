package app.deference.embycl.ui.screens.downloads

/** Presentation input; the screen does not read Session, contact a server, or access storage. */
data class DownloadsState(
    val items: List<DownloadListItem> = emptyList(),
    val query: String = "",
    val filter: DownloadFilter = DownloadFilter.ALL,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val localPlaybackEnabled: Boolean = true,
    val busyIds: Set<String> = emptySet(),
    val playingId: String? = null,
)
