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
	data class PlaybackFinished(val positionMs: Long?) : EmbyDetailsAction
}
