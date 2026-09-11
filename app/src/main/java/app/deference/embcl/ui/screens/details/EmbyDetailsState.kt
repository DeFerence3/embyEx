package app.deference.embcl.ui.screens.details

import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbySession

data class EmbyDetailsState(
	val session: EmbySession? = null,
	val content: Result<EmbyItem>? = null,
)
