package app.deference.embycl.data.repository.download

import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/** Small optional assets; one bounded attempt each. Failed assets require explicit retry. */
@Single(binds = [DownloadAssetFetcher::class])
class EmbyDownloadAssetFetcher(
    @Named("media_download") private val client: HttpClient,
    private val resolver: DownloadSourceResolver,
    private val repository: OfflineRepository,
    private val storage: OfflineStorage,
) : DownloadAssetFetcher {
    private data class AssetRequest(val target: OfflineAssetTarget, val segments: List<String>, val tag: String? = null)
    private fun targets(row: StoredDownload): List<AssetRequest> = buildList {
        val item = row.media.item
        fun image(kind: OfflineAssetKind, id: String?, type: String, tag: String?) {
            if (!id.isNullOrBlank() && !tag.isNullOrBlank()) add(AssetRequest(OfflineAssetTarget(kind, "jpg"), listOf("Items", id, "Images", type), tag))
        }
        image(OfflineAssetKind.POSTER, if (item.imageTags["Primary"] != null) item.id else item.seriesId,
            "Primary", item.imageTags["Primary"] ?: item.seriesPrimaryImageTag)
        image(OfflineAssetKind.BACKDROP, if (item.backdropImageTags.isNotEmpty()) item.id else item.parentBackdropItemId,
            "Backdrop", item.backdropImageTags.firstOrNull() ?: item.parentBackdropImageTags.firstOrNull())
        image(OfflineAssetKind.LOGO, if (item.imageTags["Logo"] != null) item.id else item.parentLogoItemId,
            "Logo", item.imageTags["Logo"] ?: item.parentLogoImageTag)
        row.source.subtitles.filter { it.availability == SubtitleAvailability.SEPARATE_FILE }.forEach { subtitle ->
            val index = subtitle.stream.index ?: return@forEach
            val format = subtitle.format ?: return@forEach
            add(AssetRequest(OfflineAssetTarget(OfflineAssetKind.SUBTITLE, format, index),
                listOf("Videos", row.task.key.itemId, row.task.key.mediaSourceId, "Subtitles", index.toString(), "0", "Stream.$format")))
        }
    }
    override suspend fun hasPending(download: StoredDownload): Boolean {
        val states = repository.observeAssets(download.task.key).first().associateBy { it.id }
        return targets(download).any { states[it.target.id]?.state in setOf(null, OfflineAssetState.PENDING) }
    }
    override suspend fun fetch(connection: DownloadConnection, download: StoredDownload) {
        require(download.task.key.accountKey() == OfflineAccountKey(connection.serverId, connection.userId))
        if (!download.isLocallyAvailable) return
        val states = repository.observeAssets(download.task.key).first().associateBy { it.id }
        val requests = targets(download).filter { states[it.target.id]?.state in setOf(null, OfflineAssetState.PENDING) }
        if (requests.isEmpty()) return
        // C2 verifies public identity before authentication even for optional assets.
        val ready = resolver.resolve(connection, download.task.key.itemId, download.task.key.mediaSourceId) as? DownloadResolution.Ready
        for (asset in requests) {
            currentCoroutineContext().ensureActive()
            val target = asset.target
            try {
                check(ready?.source?.key == download.task.key)
                if (target.kind == OfflineAssetKind.SUBTITLE) {
                    check(download.probe.strongEtag != null && ready.probe.strongEtag == download.probe.strongEtag &&
                        ready.source.subtitles == download.source.subtitles)
                }
                val request = DownloadRequests.metadata(connection, *asset.segments.toTypedArray()).apply {
                    headers.remove(HttpHeaders.Accept)
                    header(HttpHeaders.AcceptEncoding, "identity")
                    timeout { requestTimeoutMillis = 60_000 }
                    if (target.kind != OfflineAssetKind.SUBTITLE) {
                        url.parameters.append("Format", "jpg")
                        url.parameters.append("MaxWidth", if (target.kind == OfflineAssetKind.BACKDROP) "1920" else "800")
                        url.parameters.append("Tag", checkNotNull(asset.tag))
                    }
                }
                withTimeout(60_000) {
                    client.prepareRequest(request).execute { response ->
                        check(response.status.value == 200)
                        check(response.headers[HttpHeaders.ContentRange] == null)
                        val encoding = TransferProtocol.header(response.headers, HttpHeaders.ContentEncoding)
                        check(encoding == null || encoding.equals("identity", true))
                        check(response.headers[HttpHeaders.TransferEncoding] == null || response.headers[HttpHeaders.ContentLength] == null)
                        val length = TransferProtocol.length(response.headers)
                        check(length == null || length in 1..target.maxBytes)
                        val type = TransferProtocol.header(response.headers, HttpHeaders.ContentType)?.substringBefore(';')?.trim()?.lowercase()
                        if (target.kind != OfflineAssetKind.SUBTITLE) check(type == "image/jpeg")
                        else check(type in setOf("text/plain", "text/vtt", "text/x-ssa", "text/x-ass", "application/x-subrip", "application/octet-stream"))
                        storage.saveAsset(download.task.key, download.version, target, length) { writer ->
                            val channel = response.bodyAsChannel()
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val count = channel.readAvailable(buffer)
                                if (count < 0) { channel.closedCause?.let { throw it }; break }
                                if (count > 0) writer.write(buffer, 0, count)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
                // A per-asset timeout is a failed optional asset; parent cancellation propagates.
                failed(download, target)
            } catch (_: Exception) { failed(download, target) }
        }
    }
    private suspend fun failed(row: StoredDownload, target: OfflineAssetTarget) {
        repository.saveAsset(row.task.key, row.version,
            OfflineAsset(target.id, target.kind, OfflineAssetState.FAILED, streamIndex = target.streamIndex))
    }
}
