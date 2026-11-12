package app.deference.embycl.ui.screens.settings.home

import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeTop

sealed interface HomeLayoutSettingsAction {
    data class SetTop(val top: HomeTop) : HomeLayoutSettingsAction
    data class SetCardSize(val size: HomeCardSize) : HomeLayoutSettingsAction
    data class ToggleSection(val key: String) : HomeLayoutSettingsAction
    data class MoveSection(
        val key: String,
        val up: Boolean,
    ) : HomeLayoutSettingsAction
    data object Reset : HomeLayoutSettingsAction
    data object UndoReset : HomeLayoutSettingsAction
    data object ResetNoticeShown : HomeLayoutSettingsAction
}
