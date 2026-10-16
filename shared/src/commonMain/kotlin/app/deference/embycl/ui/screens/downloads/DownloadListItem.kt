package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.domain.model.download.DownloadTask

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
    val fileCheckPending: Boolean = false,
) {
    val isPlayable: Boolean
        get() = task.status == DownloadStatus.COMPLETED && fileAvailable
}
