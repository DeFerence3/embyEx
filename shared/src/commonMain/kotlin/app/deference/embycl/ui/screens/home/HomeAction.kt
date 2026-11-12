package app.deference.embycl.ui.screens.home

import app.deference.embycl.domain.model.download.DownloadKey

sealed interface HomeAction {
    data object Refresh : HomeAction
    data class SelectSlide(val index: Int) : HomeAction
    data class RetrySection(val key: String) : HomeAction
    data class ToggleWatched(val itemId: String) : HomeAction
    data class ToggleFavorite(val itemId: String) : HomeAction
    data class RemoveFromContinue(val itemId: String) : HomeAction
    data object Undo : HomeAction
    data object DismissNotice : HomeAction
    data class RequestDownload(val itemId: String) : HomeAction
    data class SelectSource(val key: DownloadKey) : HomeAction
    data object ConfirmDownload : HomeAction
    data object DismissDownload : HomeAction
}
