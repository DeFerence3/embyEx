package app.deference.embycl.domain.appupdate


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutputMetadata(
    @SerialName("version")
    val version: Int,
    @SerialName("artifactType")
    val artifactType: ArtifactType,
    @SerialName("applicationId")
    val applicationId: String,
    @SerialName("variantName")
    val variantName: String,
    @SerialName("elements")
    val elements: List<Element>,
    @SerialName("elementType")
    val elementType: String,
    @SerialName("baselineProfiles")
    val baselineProfiles: List<BaselineProfile>,
    @SerialName("minSdkVersionForDexing")
    val minSdkVersionForDexing: Int
)