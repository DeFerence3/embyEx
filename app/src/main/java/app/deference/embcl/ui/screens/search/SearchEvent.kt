package app.deference.embcl.ui.screens.search

sealed interface SearchEvent {
	data class Error(val message: String) : SearchEvent
}
