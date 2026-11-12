package app.deference.embycl.domain.model

import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionResult
data class EmbyHome(
	val libraries: List<EmbyItem> = emptyList(),
	val sections: Map<HomeSectionId, HomeSectionResult> = emptyMap(),
)
