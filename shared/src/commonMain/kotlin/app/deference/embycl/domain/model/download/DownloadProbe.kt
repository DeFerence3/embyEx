package app.deference.embycl.domain.model.download

import kotlinx.serialization.Serializable

/** Describes the range probe only; it does not establish whole-file integrity. */
@Serializable
data class DownloadProbe(
	val totalBytes: Long?,
	val rangeSupported: Boolean,
	val etag: String?,
	val lastModified: String?,
) {
	/** Weak ETags must not be used in If-Range requests. */
	val strongEtag: String?
		get() = etag?.takeIf {
			it.length >= 2 && it.startsWith('"') && it.endsWith('"') &&
					it.substring(1, it.lastIndex).all { character ->
						character.code == 0x21 || character.code in 0x23..0x7e || character.code in 0x80..0xff
					}
		}
}