package app.deference.embycl.ui.screens.details

import app.deference.embycl.domain.model.download.DownloadKey

sealed interface EmbyDetailsAction {
	data object Retry : EmbyDetailsAction
	data object Download : EmbyDetailsAction
	data class SelectSource(val key: DownloadKey) : EmbyDetailsAction
	data object ConfirmDownload : EmbyDetailsAction
	data object DismissDownload : EmbyDetailsAction
	data object RequestRemoveDownload : EmbyDetailsAction
	data object ConfirmRemoveDownload : EmbyDetailsAction
	data object DismissRemoveDownload : EmbyDetailsAction
	data object ToggleFavorite : EmbyDetailsAction
	data object ToggleWatched : EmbyDetailsAction
	data object UndoWatched : EmbyDetailsAction
	data class SelectAudio(val index: Int) : EmbyDetailsAction
	data class SelectSubtitle(val index: Int?) : EmbyDetailsAction
	data object DismissNotice : EmbyDetailsAction
}
