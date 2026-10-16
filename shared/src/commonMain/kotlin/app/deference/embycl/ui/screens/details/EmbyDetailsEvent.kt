package app.deference.embycl.ui.screens.details

sealed interface EmbyDetailsEvent {
	data class LaunchPlayback(val request: EmbyPlaybackRequest) : EmbyDetailsEvent
	data class Error(val message: String) : EmbyDetailsEvent
}

typealias EmbyPlaybackRequest = app.deference.embycl.domain.model.EmbyPlaybackRequest
