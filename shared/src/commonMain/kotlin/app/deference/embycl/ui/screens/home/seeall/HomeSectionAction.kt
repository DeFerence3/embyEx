package app.deference.embycl.ui.screens.home.seeall

sealed interface HomeSectionAction {
    data object Retry : HomeSectionAction
    data object LoadMore : HomeSectionAction
}
