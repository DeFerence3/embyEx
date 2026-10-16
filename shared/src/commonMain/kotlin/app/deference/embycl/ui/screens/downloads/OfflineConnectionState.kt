package app.deference.embycl.ui.screens.downloads

data class OfflineConnectionState(
    val serverName: String,
    val userName: String,
    val address: String,
    val status: OfflineConnectionStatus,
    val isConnecting: Boolean = false,
    val isSearching: Boolean = false,
    val discoveryFinished: Boolean = false,
    val error: String? = null,
)
