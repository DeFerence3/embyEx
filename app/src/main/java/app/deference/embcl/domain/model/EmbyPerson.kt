package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyPerson(
	@SerialName("Name")
	val name: String? = null,
	@SerialName("Role")
	val role: String? = null,
	@SerialName("Type")
	val type: String? = null,
	@SerialName("PrimaryImageTag")
	val primaryImageTag: String? = null,
	@SerialName("Id")
	val id: String? = null,
)
