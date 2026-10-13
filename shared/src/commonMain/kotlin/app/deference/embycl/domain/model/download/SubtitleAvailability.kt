package app.deference.embycl.domain.model.download

import kotlinx.serialization.Serializable

@Serializable
enum class SubtitleAvailability {
	EMBEDDED,
	SEPARATE_FILE,
	UNSUPPORTED
}