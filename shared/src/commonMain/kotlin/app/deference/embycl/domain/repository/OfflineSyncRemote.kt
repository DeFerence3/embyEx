package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.DownloadConnection
import kotlinx.serialization.json.JsonObject

data class OfflineRemoteMedia(val item: EmbyItem, val userData: JsonObject)
enum class OfflineSyncFailure { AUTHENTICATION, WRONG_SERVER, ITEM_UNAVAILABLE, UNSUPPORTED, TRANSIENT }
class OfflineSyncException(val reason: OfflineSyncFailure) : Exception(reason.name)

interface OfflineSyncRemote {
    suspend fun read(connection: DownloadConnection, itemId: String): OfflineRemoteMedia
    /** Empty successful responses are not an acknowledgement; callers must read back. */
    suspend fun write(connection: DownloadConnection, itemId: String, userData: JsonObject)
}
