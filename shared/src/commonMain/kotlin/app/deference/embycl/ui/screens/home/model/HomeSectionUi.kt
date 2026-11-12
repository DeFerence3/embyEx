package app.deference.embycl.ui.screens.home.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeSectionUi(
    val key: String,
    val kind: HomeSectionKind,
    val cards: List<HomeCardUi>,
    val total: Int?,
    val failed: Boolean,
)
