package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DownloadStatusUi {
    data object Unavailable : DownloadStatusUi

    data object Idle : DownloadStatusUi

    data object Checking : DownloadStatusUi

    data object Queued : DownloadStatusUi

    data class Downloading(val progress: Float) : DownloadStatusUi

    data class Paused(val progress: Float) : DownloadStatusUi

    data object Downloaded : DownloadStatusUi

    data object Failed : DownloadStatusUi
}
