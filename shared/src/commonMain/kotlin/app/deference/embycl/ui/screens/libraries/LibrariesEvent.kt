package app.deference.embycl.ui.screens.libraries

sealed interface LibrariesEvent {
	data class Error(val message: String) : LibrariesEvent
}
