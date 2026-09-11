package app.deference.embcl.ui.screens.home

sealed interface HomeAction {
	data object Retry : HomeAction
}
