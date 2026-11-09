package app.deference.embycl.ui.screens.details

import app.deference.embycl.ui.screens.details.model.DetailsNotice
import app.deference.embycl.ui.screens.details.model.DetailsUi
import app.deference.embycl.ui.screens.details.model.DownloadStatusUi
import app.deference.embycl.ui.screens.details.model.SeasonUi
import app.deference.embycl.ui.screens.downloads.DownloadChoice

data class EmbyDetailsState(
	val content: Result<DetailsUi>? = null,
	val download: DownloadStatusUi = DownloadStatusUi.Idle,
	val choice: DownloadChoice? = null,
	val confirmRemove: Boolean = false,
	val selectedAudioIndex: Int? = null,
	val selectedSubtitleIndex: Int? = null,
	val season: SeasonUi = SeasonUi.Hidden,
	val notice: DetailsNotice? = null,
	val isOffline: Boolean = false,
)
