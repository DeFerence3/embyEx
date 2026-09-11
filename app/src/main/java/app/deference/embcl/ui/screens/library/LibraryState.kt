package app.deference.embcl.ui.screens.library

import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbySession

data class LibraryState(
	val session: EmbySession? = null,
	val content: Result<EmbyItemsResult>? = null,
)
