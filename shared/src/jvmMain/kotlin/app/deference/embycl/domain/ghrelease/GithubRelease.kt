package app.deference.embycl.domain.ghrelease

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
	val body: String? = null,
	@SerialName("tag_name")
	val tag: String = "",
	val draft: Boolean = false,
)
