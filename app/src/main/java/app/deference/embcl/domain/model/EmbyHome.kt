package app.deference.embcl.domain.model

data class EmbyHome(
	val views: List<EmbyItem> = emptyList(),
	val resume: List<EmbyItem> = emptyList(),
	val latest: List<EmbyItem> = emptyList(),
)
