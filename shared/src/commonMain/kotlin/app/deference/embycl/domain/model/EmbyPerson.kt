package app.deference.embycl.domain.model

import app.deference.embycl.core.session.Session
import app.deference.embycl.core.utils.buildUrl
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
) {

	fun imageUrl(maxWidth: Int = 320): String? {
		val personId = id ?: return null
		val tag = primaryImageTag ?: return null
		return buildUrl(
			Session.serverUrl,
			"/Items/$personId/Images/Primary",
			mapOf(
				"MaxWidth" to maxWidth.toString(),
				"Quality" to "90",
				"Tag" to tag,
			),
		).toString()
	}
}
