package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.EmbyUser
import app.deference.embycl.domain.model.PublicSystemInfo
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.*
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Public identity first, then scoped authentication. Never uses the global Session client. */
@Single(binds = [OfflineServerConnector::class])
class OfflineServerConnectorImpl(@Named("media_download") private val client: HttpClient) : OfflineServerConnector {
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun verify(account: OfflineAccountKey, address: String, connection: DownloadConnection?): OfflineServerResult = try {
        require(connection == null || (connection.serverId == account.serverId && connection.userId == account.userId))
        val candidate = DownloadConnection(account.serverId, account.userId, address, connection?.accessToken ?: "public-probe-only")
        withTimeout(10_000) {
            val server = response(candidate, false, "System", "Info", "Public")
            if (server.first != 200) return@withTimeout OfflineServerResult.UNAVAILABLE
            if (json.decodeFromString<PublicSystemInfo>(server.second).id != account.serverId)
                return@withTimeout OfflineServerResult.WRONG_SERVER
            if (connection == null) return@withTimeout OfflineServerResult.SIGN_IN_REQUIRED
            val user = response(candidate, true, "Users", account.userId)
            when {
                user.first == 401 || user.first == 403 -> OfflineServerResult.SIGN_IN_REQUIRED
                user.first != 200 -> OfflineServerResult.UNAVAILABLE
                json.decodeFromString<EmbyUser>(user.second).id != account.userId -> OfflineServerResult.SIGN_IN_REQUIRED
                else -> OfflineServerResult.CONNECTED
            }
        }
    } catch (e: CancellationException) { currentCoroutineContext().ensureActive(); OfflineServerResult.UNAVAILABLE }
    catch (_: Exception) { OfflineServerResult.UNAVAILABLE }

    private suspend fun response(connection: DownloadConnection, authenticated: Boolean, vararg path: String): Pair<Int, String> =
        client.prepareRequest(DownloadRequests.metadata(connection, *path, authenticated = authenticated)).execute { response ->
            val channel = response.bodyAsChannel()
            val bytes = channel.readBuffer(64 * 1024L + 1).readByteArray()
            channel.closedCause?.let { throw it }
            check(bytes.size <= 64 * 1024)
            response.status.value to bytes.decodeToString()
        }
}
