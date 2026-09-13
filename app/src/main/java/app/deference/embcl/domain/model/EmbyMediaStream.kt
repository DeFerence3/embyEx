package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyMediaStream(
	@SerialName("Type")
	val type: String? = null,
	@SerialName("DisplayTitle")
	val displayTitle: String? = null,
	@SerialName("DisplayLanguage")
	val displayLanguage: String? = null,
	@SerialName("Language")
	val language: String? = null,
	@SerialName("Codec")
	val codec: String? = null,
	@SerialName("Height")
	val height: Int? = null,
	@SerialName("Width")
	val width: Int? = null,
	@SerialName("Index")
	val index: Int? = null,
	@SerialName("IsDefault")
	val isDefault: Boolean = false,
)
