package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicSystemInfo(
	@SerialName("ServerName")
	val serverName: String = "Emby",
	@SerialName("Id")
	val id: String = "",
	@SerialName("Version")
	val version: String = "",
)
