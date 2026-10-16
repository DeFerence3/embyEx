package app.deference.embycl.domain.model.download

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Structured reasons are safe to persist; raw exceptions may contain authenticated URLs. */
@Serializable
enum class DownloadFailureReason {
	@SerialName("storage_full") STORAGE_FULL,
	@SerialName("storage_unavailable") STORAGE_UNAVAILABLE,
	@SerialName("access_denied") ACCESS_DENIED,
	@SerialName("source_unavailable") SOURCE_UNAVAILABLE,
	@SerialName("invalid_response") INVALID_RESPONSE,
	@SerialName("verification_failed") VERIFICATION_FAILED,
	@SerialName("other") OTHER,
}