package app.deference.embycl.domain.appupdate


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaselineProfile(
    @SerialName("minApi")
    val minApi: Int,
    @SerialName("maxApi")
    val maxApi: Int,
    @SerialName("baselineProfiles")
    val baselineProfiles: List<String>
)