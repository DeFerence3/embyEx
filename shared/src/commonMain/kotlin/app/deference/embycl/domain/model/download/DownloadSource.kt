package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.model.EmbyMediaStream
import kotlinx.serialization.Serializable

@Serializable
data class DownloadSource(
    val key: DownloadKey,
    val name: String?,
    val container: String,
    val sizeBytes: Long?,
    val runTimeTicks: Long?,
    val streams: List<EmbyMediaStream>,
    val subtitles: List<DownloadSubtitle>,
)









