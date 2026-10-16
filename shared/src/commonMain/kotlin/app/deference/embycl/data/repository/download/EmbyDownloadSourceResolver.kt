package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyMediaSource
import app.deference.embycl.domain.model.EmbyMediaStream
import app.deference.embycl.domain.model.EmbyUser
import app.deference.embycl.domain.model.PublicSystemInfo
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.DownloadSourceResolver
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock

/** Uses the isolated media client; does not mutate Session or start a file download. */
@Single
class EmbyDownloadSourceResolver(
    @Named("media_download") private val client: HttpClient,
) : DownloadSourceResolver {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun resolve(
        connection: DownloadConnection,
        itemId: String,
        mediaSourceId: String?,
    ): DownloadResolution = try {
        resolveChecked(connection, itemId, mediaSourceId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Rejection) {
        DownloadResolution.Rejected(e.reason, e.httpStatus, e.retryAfterMillis)
    } catch (_: HttpRequestTimeoutException) {
        DownloadResolution.Rejected(DownloadRejection.NETWORK_UNAVAILABLE)
    } catch (_: ConnectTimeoutException) {
        DownloadResolution.Rejected(DownloadRejection.NETWORK_UNAVAILABLE)
    } catch (_: SocketTimeoutException) {
        DownloadResolution.Rejected(DownloadRejection.NETWORK_UNAVAILABLE)
    } catch (_: IOException) {
        DownloadResolution.Rejected(DownloadRejection.NETWORK_UNAVAILABLE)
    }

    private suspend fun resolveChecked(
        connection: DownloadConnection,
        itemId: String,
        mediaSourceId: String?,
    ): DownloadResolution {
        require(itemId.isNotBlank()) { "An item ID is required." }
        require(mediaSourceId == null || mediaSourceId.isNotBlank()) { "A selected source ID cannot be blank." }

        // An address may have been reassigned to another server. Check before sending the token.
        val server = metadata<PublicSystemInfo>(connection, "System", "Info", "Public", authenticated = false)
        if (server.id != connection.serverId) reject(DownloadRejection.WRONG_SERVER)
        val user = metadata<EmbyUser>(connection, "Users", connection.userId)
        if (user.id != connection.userId) reject(DownloadRejection.WRONG_USER)
        if (user.policy?.isDisabled == true || user.policy?.enableContentDownloading == false) {
            reject(DownloadRejection.ACCESS_DENIED)
        }

        val item = metadata<EmbyItem>(connection, "Users", connection.userId, "Items", itemId)
        if (item.id != itemId) reject(DownloadRejection.INVALID_RESPONSE)
        if (item.serverId != null && item.serverId != connection.serverId) reject(DownloadRejection.WRONG_SERVER)
        if (item.canDownload == false) reject(DownloadRejection.ACCESS_DENIED)
        if (item.canDownload != true && user.policy?.enableContentDownloading != true) {
            reject(DownloadRejection.PERMISSION_UNKNOWN)
        }
        if (item.isFolder || item.type !in setOf("Movie", "Episode") ||
            (item.mediaType != null && item.mediaType != "Video") || (item.partCount ?: 1) > 1) {
            reject(DownloadRejection.UNSUPPORTED_MEDIA)
        }
        val ids = item.mediaSources.mapNotNull { it.id }
        if (ids.size != ids.distinct().size) reject(DownloadRejection.INVALID_RESPONSE)
        val sources = item.mediaSources.mapNotNull { source -> source.toDownloadSource(connection, item) }
        val selected = if (mediaSourceId != null) {
            if (item.mediaSources.none { it.id == mediaSourceId }) reject(DownloadRejection.SOURCE_NOT_FOUND)
            sources.singleOrNull { it.key.mediaSourceId == mediaSourceId }
                ?: reject(DownloadRejection.UNSUPPORTED_SOURCE)
        } else {
            if (sources.isEmpty()) reject(DownloadRejection.UNSUPPORTED_SOURCE)
            if (sources.size > 1) return DownloadResolution.ChooseSource(item, sources)
            sources.single()
        }
        return DownloadResolution.Ready(item, selected, probe(connection, selected))
    }

    private suspend inline fun <reified T> metadata(
        connection: DownloadConnection,
        vararg segments: String,
        authenticated: Boolean = true,
    ): T {
        val response = client.request(DownloadRequests.metadata(connection, *segments, authenticated = authenticated).apply {
            timeout { requestTimeoutMillis = 15_000 }
        })
        checkStatus(response, setOf(200))
        return try {
            json.decodeFromString<T>(response.bodyAsText())
        } catch (_: SerializationException) {
            reject(DownloadRejection.INVALID_RESPONSE)
        }
    }

    private suspend fun probe(connection: DownloadConnection, source: DownloadSource): DownloadProbe {
        val request = DownloadRequests.media(connection, source.key).apply {
            header(HttpHeaders.Range, "bytes=0-0")
            timeout { requestTimeoutMillis = 15_000 }
        }
        // Scoped streaming response: an ignored Range must not buffer a whole movie.
        return client.prepareRequest(request).execute { response ->
            checkStatus(response, setOf(200, 206))
            val encoding = response.headers[HttpHeaders.ContentEncoding]
            if (encoding != null && !encoding.equals("identity", ignoreCase = true)) reject(DownloadRejection.INVALID_RESPONSE)
            val contentType = response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
            if (contentType?.startsWith("text/") == true || contentType in setOf(
                    "application/json", "application/xml", "application/vnd.apple.mpegurl",
                    "application/x-mpegurl", "application/dash+xml",
                )) reject(DownloadRejection.INVALID_RESPONSE)
            val length = response.headers[HttpHeaders.ContentLength]?.let {
                it.toLongOrNull()?.takeIf { bytes -> bytes > 0 } ?: reject(DownloadRejection.INVALID_RESPONSE)
            }
            val ranged = response.status.value == 206
            val total = if (ranged) {
                if (length != null && length != 1L) reject(DownloadRejection.INVALID_RESPONSE)
                val range = response.headers[HttpHeaders.ContentRange].orEmpty()
                val match = Regex("bytes 0-0/([0-9]+)", RegexOption.IGNORE_CASE).matchEntire(range)
                    ?: reject(DownloadRejection.INVALID_RESPONSE)
                // Check the promised byte exists, and reject an oversized range response.
                // The two-byte cap keeps even a misbehaving response bounded.
                if (response.bodyAsChannel().readBuffer(2L).readByteArray().size != 1) {
                    reject(DownloadRejection.INVALID_RESPONSE)
                }
                match.groupValues[1].toLongOrNull()?.takeIf { it > 0 }
                    ?: reject(DownloadRejection.INVALID_RESPONSE)
            } else {
                if (response.headers[HttpHeaders.ContentRange] != null) reject(DownloadRejection.INVALID_RESPONSE)
                length
            }
            if (source.sizeBytes != null && total != null && source.sizeBytes != total) {
                reject(DownloadRejection.SOURCE_CHANGED)
            }
            DownloadProbe(total, ranged, response.headers[HttpHeaders.ETag], response.headers[HttpHeaders.LastModified])
        }
    }

    private fun EmbyMediaSource.toDownloadSource(connection: DownloadConnection, item: EmbyItem): DownloadSource? {
        val sourceId = id?.takeIf { it.isNotBlank() } ?: return null
        // V1 handles finite originals stored on the Emby server, not live/channel/remote streams.
        if (protocol != "File" || (type != null && type != "Default") || isRemote ||
            isInfiniteStream || requiresOpening || requiresClosing || (size != null && size <= 0)) return null
        val extension = container?.lowercase()?.takeIf {
            it.matches(Regex("[a-z0-9]{1,12}")) && it !in setOf("m3u", "m3u8", "hls", "mpd", "ism", "strm", "part")
        } ?: return null
        return DownloadSource(
            key = DownloadKey(connection.serverId, connection.userId, item.id, sourceId),
            name = name,
            container = extension,
            sizeBytes = size,
            runTimeTicks = runTimeTicks ?: item.runTimeTicks,
            streams = mediaStreams,
            // Item-level streams may belong to a different source; never mix them in.
            subtitles = mediaStreams.filter { it.type == "Subtitle" }.map(::subtitle),
        )
    }

    private fun subtitle(stream: EmbyMediaStream): DownloadSubtitle {
        if (stream.isExternal == false && stream.isExternalUrl != true) {
            return DownloadSubtitle(stream, SubtitleAvailability.EMBEDDED)
        }
        val format = when (stream.codec?.lowercase()) {
            "subrip", "srt" -> "srt"
            "ass" -> "ass"
            "ssa" -> "ssa"
            "webvtt", "vtt" -> "vtt"
            else -> null
        }
        return if (stream.isExternal == true && stream.isExternalUrl != true &&
            stream.isTextSubtitleStream != false && stream.supportsExternalStream != false &&
            stream.index != null && stream.index >= 0 && format != null) {
            DownloadSubtitle(stream, SubtitleAvailability.SEPARATE_FILE, format)
        } else DownloadSubtitle(stream, SubtitleAvailability.UNSUPPORTED)
    }

    private fun checkStatus(response: HttpResponse, allowed: Set<Int>) {
        val code = response.status.value
        if (code in allowed) return
        reject(when (code) {
            401 -> DownloadRejection.AUTHENTICATION_REQUIRED
            403 -> DownloadRejection.ACCESS_DENIED
            404, 410 -> DownloadRejection.NOT_FOUND
            429 -> DownloadRejection.RATE_LIMITED
            in 300..399 -> DownloadRejection.REDIRECT_NOT_SUPPORTED
            in 500..599 -> DownloadRejection.SERVER_ERROR
            else -> DownloadRejection.INVALID_RESPONSE
        }, code, if (code == 429 || code in 500..599)
            TransferProtocol.retryAfter(response.headers, Clock.System.now().toEpochMilliseconds()) else null)
    }

    private class Rejection(val reason: DownloadRejection, val httpStatus: Int?, val retryAfterMillis: Long?) : Exception(reason.name)
    private fun reject(reason: DownloadRejection, status: Int? = null, retryAfterMillis: Long? = null): Nothing = throw Rejection(reason, status, retryAfterMillis)
}
