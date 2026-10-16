package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.OfflineAsset
import app.deference.embycl.domain.model.download.StoredDownload

data class SavedDownloadUi(
    val record: StoredDownload,
    val item: DownloadListItem,
    val backdrop: String? = null,
    val logo: String? = null,
    val positionTicks: Long = 0,
    val assetStates: List<OfflineAsset> = emptyList(),
)
