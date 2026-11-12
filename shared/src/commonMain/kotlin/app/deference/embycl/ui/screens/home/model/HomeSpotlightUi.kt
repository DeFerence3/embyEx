package app.deference.embycl.ui.screens.home.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeSpotlightUi(
    val kind: HomeSpotlightKind,
    val items: List<HomeCardUi>,
    val selected: Int,
) {
    val current: HomeCardUi get() = items[selected]
}
