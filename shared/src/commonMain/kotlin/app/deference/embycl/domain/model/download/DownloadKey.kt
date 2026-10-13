package app.deference.embycl.domain.model.download

import kotlinx.serialization.Serializable

/**
 * Identity of an original-quality download. IDs are opaque, case-sensitive values.
 *
 * Resolve a real server ID and media source ID before enqueueing. An address can
 * change, and an item can have several sources, so neither is a substitute for
 * these IDs. Do not concatenate them into paths or store credentials here.
 */
@Serializable
data class DownloadKey(
    val serverId: String,
    val userId: String,
    val itemId: String,
    val mediaSourceId: String,
) {
    init {
        require(serverId.isNotBlank()) { "A download requires a server ID." }
        require(userId.isNotBlank()) { "A download requires a user ID." }
        require(itemId.isNotBlank()) { "A download requires an item ID." }
        require(mediaSourceId.isNotBlank()) { "A download requires a media source ID." }
    }
}
