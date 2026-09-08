package app.deference.embcl.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmbyUdpServer(
	@SerialName("Address") val address: String,
	@SerialName("Id") val id: String,
	@SerialName("Name") val name: String,
)
