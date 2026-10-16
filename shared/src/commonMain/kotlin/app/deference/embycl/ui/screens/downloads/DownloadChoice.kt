package app.deference.embycl.ui.screens.downloads

import app.deference.embycl.domain.model.download.DownloadKey
import app.deference.embycl.domain.model.download.DownloadSource

data class DownloadChoice(
    val title: String,
    val sources: List<DownloadSource>,
    val selected: DownloadKey? = null,
    val submitting: Boolean = false,
)
