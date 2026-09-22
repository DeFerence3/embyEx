package app.deference.embycl.ui.screens.home

sealed interface HomeEvent {
	data class Error(val message: String) : HomeEvent
}
