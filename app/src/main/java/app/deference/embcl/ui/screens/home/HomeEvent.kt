package app.deference.embcl.ui.screens.home

sealed interface HomeEvent {
	data class Error(val message: String) : HomeEvent
}
