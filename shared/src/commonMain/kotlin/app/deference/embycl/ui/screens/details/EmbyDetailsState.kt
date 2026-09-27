package app.deference.embycl.ui.screens.details

import app.deference.embycl.domain.model.EmbyItem

data class EmbyDetailsState(
	val content: Result<EmbyItem>? = null,
)
