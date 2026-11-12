package app.deference.embycl.ui.screens.home.seeall

import app.deference.embycl.domain.model.EmbyItem

data class HomeSectionState(
    val items: List<EmbyItem>? = null,
    val total: Int? = null,
    val failed: Boolean = false,
    val loadingMore: Boolean = false,
) {
    val canLoadMore: Boolean get() = !loadingMore && items != null && total != null && items.size < total
}
