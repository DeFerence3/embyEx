package app.deference.embycl.ui.screens.details

sealed interface EmbyDetailsAction {
	data object Retry : EmbyDetailsAction
	data object Play : EmbyDetailsAction
	data object PlayFromServer : EmbyDetailsAction
	data object Download : EmbyDetailsAction
	data class SelectSource(val key: app.deference.embycl.domain.model.download.DownloadKey) : EmbyDetailsAction
	data object ConfirmDownload : EmbyDetailsAction
	data object DismissDownload : EmbyDetailsAction
	data object RemoveDownload : EmbyDetailsAction
    data class PlaybackUpdate(
        val request: app.deference.embycl.domain.model.EmbyPlaybackRequest,
        val position: app.deference.embycl.domain.model.PlaybackPosition,
        val finished: Boolean = false,
    ) : EmbyDetailsAction
    data object RetryPlaybackSave : EmbyDetailsAction
    data object DismissPlaybackSaveError : EmbyDetailsAction
}
