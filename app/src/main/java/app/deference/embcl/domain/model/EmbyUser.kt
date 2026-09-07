package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyUser(
	@SerialName("Id")
	val id: String,
	@SerialName("Name")
	val name: String,
	@SerialName("ServerName")
	val serverName: String? = null,
	@SerialName("PrimaryImageTag")
	val primaryImageTag: String? = null,
	@SerialName("HasPassword")
	val hasPassword: Boolean = false,
	@SerialName("HasConfiguredPassword")
	val hasConfiguredPassword: Boolean = false,
)
