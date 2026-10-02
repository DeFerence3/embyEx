package app.deference.embycl.domain.appupdate

import app.deference.embycl.domain.model.update.UpdateRelease
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
	val release: UpdateRelease,
	val url: String,
	val digest: String?,
	val size: Long
)
