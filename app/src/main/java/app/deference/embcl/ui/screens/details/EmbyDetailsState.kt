package app.deference.embcl.ui.screens.details

import app.deference.embcl.domain.model.EmbyItem

data class EmbyDetailsState(
	val content: Result<EmbyItem>? = null,
)
