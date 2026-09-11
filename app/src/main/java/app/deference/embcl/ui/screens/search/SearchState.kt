package app.deference.embcl.ui.screens.search

import app.deference.embcl.domain.model.EmbyItem

data class SearchState(
	val query: String = "",
	val results: List<EmbyItem> = emptyList(),
	val isLoading: Boolean = false,
	val error: String? = null,
)
