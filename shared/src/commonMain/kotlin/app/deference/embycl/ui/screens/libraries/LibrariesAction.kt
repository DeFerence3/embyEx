package app.deference.embycl.ui.screens.libraries

sealed interface LibrariesAction {
	data object Retry : LibrariesAction
}
