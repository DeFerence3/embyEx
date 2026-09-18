package app.deference.embcl.ui.screens.library

import app.deference.embcl.domain.model.EmbyItemsResult

data class LibraryState(
	val content: Result<EmbyItemsResult>? = null,
)
