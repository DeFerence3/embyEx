package app.deference.embcl.ui.screens.details

sealed interface EmbyDetailsEvent {
	data class LaunchPlayback(val request: EmbyPlaybackRequest) : EmbyDetailsEvent
	data class Error(val message: String) : EmbyDetailsEvent
}

data class EmbyPlaybackRequest(
	val itemId: String,
	val title: String,
	val urls: ArrayList<String>,
	val selectedIndex: Int,
	val positionMs: Int,
	val accessToken: String,
)
