package app.deference.embycl.ui.screens.home

sealed interface HomeAction {
	data object Retry : HomeAction
	data object Refresh : HomeAction
}
