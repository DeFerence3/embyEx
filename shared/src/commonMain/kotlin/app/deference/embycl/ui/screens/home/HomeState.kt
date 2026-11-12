package app.deference.embycl.ui.screens.home

import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.ui.screens.downloads.DownloadChoice
import app.deference.embycl.ui.screens.home.model.HomeContentUi
import app.deference.embycl.ui.screens.home.model.HomeFullState
import app.deference.embycl.ui.screens.home.model.HomeNotice
import app.deference.embycl.ui.screens.home.model.HomeSourceUi

data class HomeState(
    val content: HomeContentUi? = null,
    val fullState: HomeFullState? = null,
    val source: HomeSourceUi = HomeSourceUi.Connecting,
    val serverName: String = "",
    val serverAddress: String = "",
    val isRefreshing: Boolean = false,
    val cardSize: HomeCardSize = HomeCardSize.Comfortable,
    val downloadChoice: DownloadChoice? = null,
    val notice: HomeNotice? = null,
) {
    val isLoading: Boolean get() = content == null && fullState == null
    val canChangeOnServer: Boolean get() = source == HomeSourceUi.Connected
}
