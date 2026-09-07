package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationResult(
	@SerialName("User")
	val user: EmbyUser,
	@SerialName("AccessToken")
	val accessToken: String,
	@SerialName("ServerId")
	val serverId: String = "",
)
