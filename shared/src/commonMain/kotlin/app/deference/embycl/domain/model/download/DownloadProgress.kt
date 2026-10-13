package app.deference.embycl.domain.model.download

import kotlinx.serialization.Serializable

/** Byte counts describe the media payload, excluding artwork and sidecar files. */
@Serializable
data class DownloadProgress(
    val downloadedBytes: Long = 0,
    /** Null means unknown. An empty response is not a valid media download. */
    val totalBytes: Long? = null,
) {
    init {
        require(downloadedBytes >= 0) { "Downloaded bytes cannot be negative." }
        require(totalBytes == null || totalBytes > 0) { "Known media size must be positive." }
        require(totalBytes == null || downloadedBytes <= totalBytes) {
            "Downloaded bytes cannot exceed the expected media size."
        }
    }

    /** A full progress bar does not imply that the file is verified or playable. */
    val fraction: Float?
        get() = totalBytes?.let { (downloadedBytes.toDouble() / it).toFloat() }

    internal val canVerify: Boolean
        get() = downloadedBytes > 0 && (totalBytes == null || downloadedBytes == totalBytes)
}
