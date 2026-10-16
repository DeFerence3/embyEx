package app.deference.embycl.ui.screens.downloads

sealed interface DownloadsUiEvent {
    data class Details(
        val id: String,
    ) : DownloadsUiEvent

    data object Downloads : DownloadsUiEvent

    data object Library : DownloadsUiEvent

    data object SignIn : DownloadsUiEvent
}
