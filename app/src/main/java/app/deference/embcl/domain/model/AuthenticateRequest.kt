package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticateRequest(
	@SerialName("Username")
	val username: String,
	@SerialName("Password")
	val password: String,
	@SerialName("Pw")
	val pw: String = password,
)
