package app.deference.embycl.ui.screens.home.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeContentUi(
    val spotlight: HomeSpotlightUi?,
    val sections: List<HomeSectionUi>,
)
