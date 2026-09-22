package app.deference.embycl.ui.screens.search

sealed interface SearchEvent {
	data class Error(val message: String) : SearchEvent
}
