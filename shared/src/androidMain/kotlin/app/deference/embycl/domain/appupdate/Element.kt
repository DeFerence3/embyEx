package app.deference.embycl.domain.appupdate


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Element(
	@SerialName("type")
    val type: String,
	@SerialName("filters")
    val filters: List<String?>,
	@SerialName("attributes")
    val attributes: List<String?>,
	@SerialName("versionCode")
    val versionCode: Int,
	@SerialName("versionName")
    val versionName: String,
	@SerialName("outputFile")
    val outputFile: String
)