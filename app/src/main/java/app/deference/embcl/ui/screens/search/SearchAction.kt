package app.deference.embcl.ui.screens.search

sealed interface SearchAction {
	data class QueryChanged(val query: String) : SearchAction
	data object Retry : SearchAction
}
