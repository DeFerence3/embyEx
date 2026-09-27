package app.deference.embycl.ui.screens.library

sealed interface LibraryAction {
	data object Retry : LibraryAction
}
