package app.deference.embcl.ui.screens.libraries

sealed interface LibrariesEvent {
	data class Error(val message: String) : LibrariesEvent
}
