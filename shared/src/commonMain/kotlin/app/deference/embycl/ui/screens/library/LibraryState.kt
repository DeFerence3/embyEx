package app.deference.embycl.ui.screens.library

import app.deference.embycl.domain.model.EmbyItemsResult

data class LibraryState(
	val content: Result<EmbyItemsResult>? = null,
)
