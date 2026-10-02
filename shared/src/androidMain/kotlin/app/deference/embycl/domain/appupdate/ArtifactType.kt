package app.deference.embycl.domain.appupdate


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtifactType(
    @SerialName("type")
    val type: String,
    @SerialName("kind")
    val kind: String
)