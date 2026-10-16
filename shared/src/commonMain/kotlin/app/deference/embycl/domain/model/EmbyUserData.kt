package app.deference.embycl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyUserData(
	@SerialName("PlaybackPositionTicks")
	val playbackPositionTicks: Long = 0,
	@SerialName("PlayedPercentage")
	val playedPercentage: Double? = null,
	@SerialName("Played")
	val played: Boolean = false,
	@SerialName("IsFavorite")
	val isFavorite: Boolean = false,
	@SerialName("PlayCount")
	val playCount: Int? = null,
	@SerialName("LastPlayedDate")
	val lastPlayedDate: kotlin.time.Instant? = null,
)
