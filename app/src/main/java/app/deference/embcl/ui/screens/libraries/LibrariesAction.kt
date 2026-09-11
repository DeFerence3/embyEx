package app.deference.embcl.ui.screens.libraries

sealed interface LibrariesAction {
	data object Retry : LibrariesAction
}
