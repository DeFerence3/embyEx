package app.deference.embycl.domain.model.download

/**
 * An in-memory account snapshot, supplied afresh for each resolution/transfer.
 * Never persist this object. It is intentionally neither a data class nor serializable.
 */
class DownloadConnection(
    val serverId: String,
    val userId: String,
    val baseUrl: String,
    internal val accessToken: String,
) {
    init {
        require(serverId.isNotBlank() && userId.isNotBlank()) { "An account identity is required." }
        require(accessToken.isNotBlank() && accessToken.none { it == '\r' || it == '\n' }) {
            "A valid access token is required."
        }
    }

    override fun toString(): String = "DownloadConnection(redacted)"
}
