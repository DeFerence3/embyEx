package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.*
import kotlinx.io.readByteArray
import kotlinx.serialization.json.*
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single(binds = [OfflineSyncRemote::class])
class EmbyOfflineSyncRemote(@Named("media_download") private val client: HttpClient,
    private val connector: OfflineServerConnector) : OfflineSyncRemote {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(connection: DownloadConnection, itemId: String): OfflineRemoteMedia = bounded {
        verify(connection)
        val body = response(DownloadRequests.metadata(connection, "Users", connection.userId, "Items", itemId))
        val root = json.parseToJsonElement(body).jsonObject
        val item = json.decodeFromJsonElement<EmbyItem>(root)
        if (item.id != itemId || (item.serverId != null && item.serverId != connection.serverId)) fail(OfflineSyncFailure.WRONG_SERVER)
        val userData = root["UserData"] as? JsonObject ?: fail(OfflineSyncFailure.UNSUPPORTED)
        val state = item.userData ?: fail(OfflineSyncFailure.UNSUPPORTED)
        if (state.playbackPositionTicks < 0 || (state.playCount ?: 0) < 0) fail(OfflineSyncFailure.UNSUPPORTED)
        // Persist only documented user-data fields, never arbitrary server extensions/URLs.
        OfflineRemoteMedia(item, JsonObject(userData.filterKeys { it in userDataFields }))
    }

    override suspend fun write(connection: DownloadConnection, itemId: String, userData: JsonObject) {
        bounded {
            verify(connection)
            response(DownloadRequests.metadata(connection, "Users", connection.userId, "Items", itemId, "UserData").apply {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(userData.toString())
            })
        }
    }

    private suspend fun verify(connection: DownloadConnection) {
        when (connector.verify(OfflineAccountKey(connection.serverId, connection.userId), connection.baseUrl, connection)) {
            OfflineServerResult.CONNECTED -> Unit
            OfflineServerResult.SIGN_IN_REQUIRED -> fail(OfflineSyncFailure.AUTHENTICATION)
            OfflineServerResult.WRONG_SERVER -> fail(OfflineSyncFailure.WRONG_SERVER)
            OfflineServerResult.UNAVAILABLE -> fail(OfflineSyncFailure.TRANSIENT)
        }
    }
    private suspend fun response(request: HttpRequestBuilder): String = client.prepareRequest(request).execute { response ->
        when (response.status.value) {
            200, 204 -> Unit
            401 -> fail(OfflineSyncFailure.AUTHENTICATION)
            403, 404, 410 -> fail(OfflineSyncFailure.ITEM_UNAVAILABLE)
            400, 405, 501 -> fail(OfflineSyncFailure.UNSUPPORTED)
            else -> fail(OfflineSyncFailure.TRANSIENT)
        }
        val channel = response.bodyAsChannel()
        val bytes = channel.readBuffer(1024 * 1024L + 1).readByteArray()
        channel.closedCause?.let { throw it }
        if (bytes.size > 1024 * 1024) fail(OfflineSyncFailure.UNSUPPORTED)
        bytes.decodeToString()
    }
    private suspend fun <T> bounded(block: suspend () -> T): T = try { withTimeout(20_000) { block() } }
    catch (e: OfflineSyncException) { throw e }
    catch (e: CancellationException) { currentCoroutineContext().ensureActive(); fail(OfflineSyncFailure.TRANSIENT) }
    catch (_: Exception) { fail(OfflineSyncFailure.TRANSIENT) }
    private fun fail(reason: OfflineSyncFailure): Nothing = throw OfflineSyncException(reason)

    private val userDataFields = setOf("Rating", "PlayedPercentage", "UnplayedItemCount", "PlaybackPositionTicks",
        "PlayCount", "IsFavorite", "LastPlayedDate", "Played", "Key", "ItemId", "ServerId")
}
