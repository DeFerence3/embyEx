package app.deference.embycl.domain.model.home

import app.deference.embycl.domain.model.EmbyItem

sealed interface HomeSectionResult {
    data class Loaded(
        val items: List<EmbyItem>,
        val total: Int?,
    ) : HomeSectionResult
    data object Failed : HomeSectionResult
}
