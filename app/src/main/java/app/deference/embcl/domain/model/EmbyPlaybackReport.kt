package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyPlaybackReport(
	@SerialName("ItemId")
	val itemId: String,
	@SerialName("PositionTicks")
	val positionTicks: Long,
	@SerialName("PlaySessionId")
	val playSessionId: String? = null,
	@SerialName("IsPaused")
	val isPaused: Boolean = false,
	@SerialName("CanSeek")
	val canSeek: Boolean = true,
	@SerialName("PlayMethod")
	val playMethod: String = "DirectPlay",
)
