package app.deference.embycl.ui.screens.details

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.screens.downloads.DownloadChoice

data class EmbyDetailsState(
	val content: Result<EmbyItem>? = null,
	val choice: DownloadChoice? = null,
	val checkingDownload: Boolean = false,
    val playback: app.deference.embycl.domain.model.EmbyPlaybackRequest? = null,
    val preparingPlayback: Boolean = false,
    val savingPlayback: Boolean = false,
    val playbackSaveFailed: Boolean = false,
)
