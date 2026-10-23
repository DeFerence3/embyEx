package app.deference.embycl.ui.screens.details

sealed interface EmbyDetailsEvent {
    data object PlaybackFailed : EmbyDetailsEvent
    data class Error(val message: String) : EmbyDetailsEvent
}
