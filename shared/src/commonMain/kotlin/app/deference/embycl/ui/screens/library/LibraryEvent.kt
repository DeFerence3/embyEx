package app.deference.embycl.ui.screens.library

sealed interface LibraryEvent {
	data class Error(val message: String) : LibraryEvent
}
