package app.deference.embcl.ui.screens.library

sealed interface LibraryAction {
	data object Retry : LibraryAction
}
