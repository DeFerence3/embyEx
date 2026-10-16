package app.deference.embycl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Deliberately excludes server paths, delivery URLs, open tokens, and HTTP headers. */
@Serializable
data class EmbyMediaSource(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Protocol") val protocol: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("IsRemote") val isRemote: Boolean = false,
    @SerialName("IsInfiniteStream") val isInfiniteStream: Boolean = false,
    @SerialName("RequiresOpening") val requiresOpening: Boolean = false,
    @SerialName("RequiresClosing") val requiresClosing: Boolean = false,
    @SerialName("MediaStreams") val mediaStreams: List<EmbyMediaStream> = emptyList(),
)
