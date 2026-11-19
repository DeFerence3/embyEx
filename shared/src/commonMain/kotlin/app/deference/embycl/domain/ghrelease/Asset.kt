package app.deference.embycl.domain.ghrelease

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    val name: String,
    val size: Long,
    val digest: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)
