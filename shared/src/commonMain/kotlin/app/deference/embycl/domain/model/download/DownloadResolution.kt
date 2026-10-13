package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.model.EmbyItem

sealed interface DownloadResolution {
	data class Ready(val item: EmbyItem, val source: DownloadSource, val probe: DownloadProbe) : DownloadResolution
	data class ChooseSource(val item: EmbyItem, val sources: List<DownloadSource>) : DownloadResolution
	data class Rejected(val reason: DownloadRejection, val httpStatus: Int? = null) : DownloadResolution
}