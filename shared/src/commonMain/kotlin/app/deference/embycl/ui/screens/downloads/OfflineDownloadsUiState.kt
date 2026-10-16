package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.OfflineAccount
import app.deference.embycl.domain.model.download.PendingPlaybackSync

data class OfflineDownloadsUiState(
    val account: OfflineAccount? = null,
    val downloads: DownloadsState = DownloadsState(
        isLoading = true,
        isOffline = true,
        localPlaybackEnabled = localPlaybackSupported,
    ),
    val saved: List<SavedDownloadUi> = emptyList(),
    val connection: OfflineConnectionState = OfflineConnectionState(
        serverName = "Emby",
        userName = "",
        address = "",
        status = OfflineConnectionStatus.UNAVAILABLE,
    ),
    val panelOpen: Boolean = false,
    val choice: DownloadChoice? = null,
    val checkingItem: String? = null,
    val busyIds: Set<String> = emptySet(),
    val unmeteredOnly: Boolean = false,
    val message: String? = null,
    val playingId: String? = null,
    val pendingSync: List<PendingPlaybackSync> = emptyList(),
    val syncPanelOpen: Boolean = false,
)
