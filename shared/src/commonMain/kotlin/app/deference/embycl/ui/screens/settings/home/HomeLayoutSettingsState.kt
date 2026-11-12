package app.deference.embycl.ui.screens.settings.home

import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeLayoutSync
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.ui.screens.settings.home.model.HomeLayoutRowUi

/**
 * Edit Home page. [sync] already reads LocalOnly while the server can't take changes.
 * [resetNotice]: the layout was just reset and can be undone; cleared once shown or acted on.
 */
data class HomeLayoutSettingsState(
    val isLoading: Boolean = true,
    val top: HomeTop = HomeTop.ContinueWatching,
    val cardSize: HomeCardSize = HomeCardSize.Comfortable,
    val rows: List<HomeLayoutRowUi> = emptyList(),
    val sync: HomeLayoutSync = HomeLayoutSync.Saved,
    val resetNotice: Boolean = false,
)
