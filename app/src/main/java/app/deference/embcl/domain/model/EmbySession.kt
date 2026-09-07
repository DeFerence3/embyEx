package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbySession(
	@SerialName("serverUrl")
	val serverUrl: String,
	@SerialName("serverName")
	val serverName: String,
	@SerialName("serverId")
	val serverId: String,
	@SerialName("userId")
	val userId: String,
	@SerialName("userName")
	val userName: String,
	@SerialName("accessToken")
	val accessToken: String,
	@SerialName("deviceId")
	val deviceId: String,
)
