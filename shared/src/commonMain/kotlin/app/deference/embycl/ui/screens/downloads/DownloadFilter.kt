package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.DownloadStatus

enum class DownloadFilter(
    val label: String,
) {
    ALL("All",),
    READY("Ready to watch"),
    IN_PROGRESS("In progress"),
    NEEDS_ATTENTION("Needs attention");

    fun includes(
        item: DownloadListItem,
    ): Boolean = when (this) {
        ALL -> true
        READY -> item.isPlayable
        IN_PROGRESS -> item.task.status in setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.VERIFYING,
        )
        NEEDS_ATTENTION -> !item.isPlayable && !IN_PROGRESS.includes(item = item)
    }
}
