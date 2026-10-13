package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.domain.model.download.DownloadTask

/** Presentation input; the screen does not read Session, contact a server, or access storage. */
data class DownloadsState(
    val items: List<DownloadListItem> = emptyList(),
    val query: String = "",
    val filter: DownloadFilter = DownloadFilter.ALL,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
)

data class DownloadListItem(
    /** Stable local row identity, independent of title and remote item IDs. */
    val id: String,
    val title: String,
    val subtitle: String,
    val task: DownloadTask,
    /** Only a local artwork model; a missing image uses the built-in placeholder. */
    val localArtwork: String? = null,
    val fileAvailable: Boolean = false,
    val resumeLabel: String? = null,
) {
    val isPlayable get() = task.status == DownloadStatus.COMPLETED && fileAvailable
}

enum class DownloadFilter(val label: String) {
    ALL("All"), READY("Ready to watch"), IN_PROGRESS("In progress"), NEEDS_ATTENTION("Needs attention");

    fun includes(item: DownloadListItem): Boolean = when (this) {
        ALL -> true
        READY -> item.isPlayable
        IN_PROGRESS -> item.task.status in setOf(
            DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING,
        )
        NEEDS_ATTENTION -> !item.isPlayable && !IN_PROGRESS.includes(item)
    }
}
