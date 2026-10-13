package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.model.EmbyMediaStream
import kotlinx.serialization.Serializable

@Serializable
data class DownloadSubtitle(
	val stream: EmbyMediaStream,
	val availability: SubtitleAvailability,
	/** A supported sidecar format, not a remote URL or filename. */
	val format: String? = null,
)