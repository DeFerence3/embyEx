package app.deference.embcl.ui.screens.details

sealed interface EmbyDetailsAction {
	data object Retry : EmbyDetailsAction
	data object Play : EmbyDetailsAction
	data class PlaybackFinished(val positionMs: Long?) : EmbyDetailsAction
}
