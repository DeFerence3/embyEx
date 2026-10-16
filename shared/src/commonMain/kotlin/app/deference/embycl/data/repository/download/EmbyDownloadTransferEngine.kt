package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.io.IOException
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.TimeSource

/** One media transfer under a C6 lease. No scheduler, Session mutation or UI side effects. */
@Single(binds = [DownloadTransferEngine::class])
class EmbyDownloadTransferEngine(
    @Named("media_download") private val client: HttpClient,
    private val resolver: DownloadSourceResolver,
    private val repository: OfflineRepository,
    private val files: OfflineFileStore,
    private val policy: DownloadTransferPolicy = DownloadTransferPolicy(),
) : DownloadTransferEngine {
    override suspend fun transfer(connection: DownloadConnection, key: DownloadKey, expected: DownloadVersion): DownloadTransferResult {
        require(connection.serverId == key.serverId && connection.userId == key.userId) { "Download account does not match." }
        return try {
            files.withDownload(expected.downloadId) { directory ->
                val row = repository.getDownload(key)?.takeIf { it.version == expected }
                    ?: return@withDownload DownloadTransferResult.Skipped(TransferSkipReason.STALE)
                if (row.task.status != DownloadStatus.QUEUED) return@withDownload DownloadTransferResult.Skipped(TransferSkipReason.NOT_QUEUED)
                if (directory.removalPending()) throw OfflineStorageException(OfflineStorageFailure.REMOVAL_PENDING)
                val attempt = Attempt(row, directory)
                try {
                    attempt.update { it.change(task = it.task.start()) }
                    attempt.run(connection)
                } catch (e: TransferStop) {
                    attempt.update { it.change(task = if (e.authentication) it.task.waitForAuthentication() else it.task.fail(checkNotNull(e.failure))) }
                } catch (e: OfflineStorageException) {
                    attempt.update { it.change(task = it.task.fail(when (e.reason) {
                        OfflineStorageFailure.FULL -> DownloadFailureReason.STORAGE_FULL
                        OfflineStorageFailure.SIZE_MISMATCH -> DownloadFailureReason.VERIFICATION_FAILED
                        else -> DownloadFailureReason.STORAGE_UNAVAILABLE
                    })) }
                }
                DownloadTransferResult.Saved(attempt.row)
            }
        } catch (_: TransferStale) {
            currentCoroutineContext().ensureActive()
            DownloadTransferResult.Skipped(TransferSkipReason.STALE)
        } catch (e: OfflineStorageException) {
            DownloadTransferResult.Skipped(TransferSkipReason.STORAGE, e.reason)
        }
    }

    private inner class Attempt(var row: StoredDownload, val directory: DownloadFiles) {
        suspend fun update(change: (StoredDownload) -> DownloadChange) {
            row = repository.updateDownload(row.task.key, row.version, change) ?: throw TransferStale()
        }
        suspend fun progress(bytes: Long) {
            if (bytes != row.task.progress.downloadedBytes) update { it.change(task = it.task.updateProgress(bytes)) }
        }
        suspend fun run(connection: DownloadConnection) {
            for (index in 0 until policy.maxAttempts) {
                try {
                    val ready = resolve(connection)
                    prepare(ready)
                    download(connection)
                    // Streaming scope and file writer have closed; EOF, size checks and sync succeeded.
                    update { it.change(task = it.task.beginVerification()) }
                    val file = directory.finalizeMedia(row.source.container, row.task.progress.downloadedBytes)
                    update { it.change(task = it.task.completeAfterVerification(), relativeMediaPath = file.relativePath,
                        localMediaState = LocalMediaState.AVAILABLE) }
                    return
                } catch (e: TransferRetry) {
                    if (e.restart) {
                        directory.discardMedia(row.source.container)
                        update { it.change(task = it.task.restartFromBeginning(it.task.progress.totalBytes)) }
                    }
                    val wait = maxOf(policy.backoff(index), e.retryAfterMillis)
                    if (index == policy.maxAttempts - 1 || wait > policy.maxInlineWaitMillis) {
                        if (e.failure != null) throw TransferStop(e.failure)
                        val now = Clock.System.now().toEpochMilliseconds()
                        val retryAt = if (wait > Long.MAX_VALUE - now) Long.MAX_VALUE else now + wait
                        update { it.change(task = it.task.waitForNetwork(retryAt)) }
                        return
                    }
                    delay(wait)
                }
            }
        }

        private suspend fun resolve(connection: DownloadConnection): DownloadResolution.Ready {
            // This rechecks public server identity before any authenticated request, permissions,
            // the exact selected source and a bounded range probe on every attempt.
            return when (val result = resolver.resolve(connection, row.task.key.itemId, row.task.key.mediaSourceId)) {
                is DownloadResolution.Ready -> result.also {
                    if (it.source.key != row.task.key || it.item.id != row.task.key.itemId ||
                        !it.source.container.matches(Regex("[a-z0-9]{1,12}")) || it.source.container == "part") TransferProtocol.invalid()
                }
                is DownloadResolution.ChooseSource -> throw TransferStop(DownloadFailureReason.SOURCE_UNAVAILABLE)
                is DownloadResolution.Rejected -> when (result.reason) {
                    DownloadRejection.AUTHENTICATION_REQUIRED -> throw TransferStop(authentication = true)
                    DownloadRejection.ACCESS_DENIED, DownloadRejection.PERMISSION_UNKNOWN -> throw TransferStop(DownloadFailureReason.ACCESS_DENIED)
                    DownloadRejection.NETWORK_UNAVAILABLE, DownloadRejection.SERVER_ERROR, DownloadRejection.RATE_LIMITED ->
                        throw TransferRetry(retryAfterMillis = result.retryAfterMillis ?: 0)
                    DownloadRejection.SOURCE_CHANGED -> throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)
                    DownloadRejection.INVALID_RESPONSE, DownloadRejection.REDIRECT_NOT_SUPPORTED,
                    DownloadRejection.WRONG_SERVER, DownloadRejection.WRONG_USER -> TransferProtocol.invalid()
                    else -> throw TransferStop(DownloadFailureReason.SOURCE_UNAVAILABLE)
                }
            }
        }

        private suspend fun prepare(ready: DownloadResolution.Ready) {
            val total = ready.probe.totalBytes ?: ready.source.sizeBytes
            if (total != null && total <= 0) TransferProtocol.invalid()
            val offset = directory.partialSize()
            val sameSource = row.source.container == ready.source.container && row.source.streams == ready.source.streams &&
                row.source.subtitles == ready.source.subtitles &&
                (row.task.progress.totalBytes == null || total == row.task.progress.totalBytes)
            val sameTag = row.probe.strongEtag != null && row.probe.strongEtag == ready.probe.strongEtag
            val resume = offset > 0 && (total == null || offset < total) && sameSource && sameTag &&
                row.probe.rangeSupported && ready.probe.rangeSupported
            if (!resume) {
                if (!sameSource || !sameTag) {
                    // A changed representation may have different subtitle streams/artwork too.
                    directory.deletePayloads()
                    repository.observeAssets(row.task.key).first().forEach { asset ->
                        if (!repository.saveAsset(row.task.key, row.version, asset.copy(
                                state = OfflineAssetState.FAILED, relativePath = null, sizeBytes = null))) throw TransferStale()
                    }
                } else directory.discardMedia(row.source.container)
            }
            // Files are reconciled before changing the persisted source/byte interpretation.
            update {
                val task = if (resume) it.task.reconcilePartialBytes(offset).start().updateProgress(offset, total)
                    else it.task.restartFromBeginning(total)
                it.change(task = task, source = ready.source, probe = ready.probe.copy(totalBytes = total), relativeMediaPath = null)
            }
            repository.refreshMedia(row.task.key.mediaKey(), ready.item)
        }

        private suspend fun download(connection: DownloadConnection) {
            val offset = row.task.progress.downloadedBytes
            val requestedTag = row.probe.strongEtag
            val request = DownloadRequests.media(connection, row.task.key).apply {
                if (offset > 0) {
                    header(HttpHeaders.Range, "bytes=$offset-")
                    header(HttpHeaders.IfRange, checkNotNull(requestedTag))
                } else requestedTag?.let { header(HttpHeaders.IfMatch, it) }
                timeout { requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
            }
            streaming(request) { response ->
                val status = response.status.value
                when (status) {
                    200, 206 -> Unit
                    401 -> throw TransferStop(authentication = true)
                    403 -> throw TransferStop(DownloadFailureReason.ACCESS_DENIED)
                    404, 410 -> throw TransferStop(DownloadFailureReason.SOURCE_UNAVAILABLE)
                    412 -> throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)
                    416 -> {
                        if (offset == 0L) TransferProtocol.invalid()
                        TransferProtocol.unsatisfiedRange(response.headers)
                        // Equal disk/server lengths alone cannot establish successful EOF.
                        throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)
                    }
                    408, 429, in 500..599 -> throw TransferRetry(retryAfterMillis = TransferProtocol.retryAfter(
                        response.headers, Clock.System.now().toEpochMilliseconds()))
                    else -> TransferProtocol.invalid()
                }
                TransferProtocol.validateBody(response.headers)
                val etag = TransferProtocol.header(response.headers, HttpHeaders.ETag)
                val modified = TransferProtocol.header(response.headers, HttpHeaders.LastModified)
                val total = if (status == 206) {
                    if (offset == 0L) TransferProtocol.invalid()
                    val total = TransferProtocol.range(response.headers, offset)
                    if (etag != requestedTag) throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)
                    total
                } else {
                    if (response.headers[HttpHeaders.ContentRange] != null) TransferProtocol.invalid()
                    if (requestedTag != null && etag != null && requestedTag != etag)
                        throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)
                    TransferProtocol.length(response.headers) ?: row.task.progress.totalBytes
                }
                if (row.task.progress.totalBytes != null && total != row.task.progress.totalBytes)
                    throw TransferRetry(DownloadFailureReason.SOURCE_UNAVAILABLE, restart = true)

                val restart = status == 200 && offset > 0
                if (restart) directory.discardMedia(row.source.container)
                val start = if (restart) 0 else offset
                update {
                    val task = if (restart) it.task.restartFromBeginning(total) else it.task.updateProgress(start, total)
                    it.change(task = task, probe = DownloadProbe(total,
                        status == 206 || (!restart && it.probe.rangeSupported), etag, modified))
                }
                val channel = response.bodyAsChannel()
                var lastProgress = TimeSource.Monotonic.markNow()
                directory.writeMedia(start, total) { writer ->
                    try {
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = read(channel, buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            if (total != null && count > total - writer.bytesWritten)
                                throw TransferStop(DownloadFailureReason.VERIFICATION_FAILED)
                            writer.write(buffer, 0, count)
                            if (lastProgress.elapsedNow().inWholeMilliseconds >= policy.progressIntervalMillis) {
                                progress(writer.bytesWritten)
                                lastProgress = TimeSource.Monotonic.markNow()
                            }
                        }
                        if (writer.bytesWritten == 0L || (total != null && writer.bytesWritten != total))
                            throw TransferRetry(DownloadFailureReason.VERIFICATION_FAILED)
                    } finally {
                        // Includes network failure and cancellation; the file store closes next.
                        withContext(NonCancellable) { progress(writer.bytesWritten) }
                    }
                }
            }
        }
    }

    private suspend fun read(channel: ByteReadChannel, buffer: ByteArray): Int = try {
        withTimeout(policy.readTimeoutMillis) {
            channel.readAvailable(buffer, 0, buffer.size).also { count ->
                // Ktor readAvailable can return -1 for an exceptionally closed channel too.
                // In particular, unknown Content-Length must not turn a disconnect into EOF.
                if (count < 0) channel.closedCause?.let { throw it }
            }
        }
    } catch (_: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive(); throw TransferRetry()
    } catch (_: SocketTimeoutException) { throw TransferRetry() }
    catch (_: IOException) { throw TransferRetry() }
    catch (_: CancellationException) { currentCoroutineContext().ensureActive(); throw TransferRetry() }

    private suspend fun <T> streaming(request: HttpRequestBuilder, block: suspend (HttpResponse) -> T): T = supervisorScope {
        val headers = CompletableDeferred<Unit>()
        var entered = false
        val executing = async {
            try {
                client.prepareRequest(request).execute { response -> entered = true; headers.complete(Unit); block(response) }
            } catch (_: ConnectTimeoutException) { throw TransferRetry() }
            catch (_: SocketTimeoutException) { throw TransferRetry() }
            catch (_: HttpRequestTimeoutException) { throw TransferRetry() }
            catch (e: IOException) { if (!entered) throw TransferRetry() else throw e }
        }
        try {
            withTimeout(policy.headerTimeoutMillis) {
                select<Unit> { headers.onAwait { }; executing.onAwait { } }
            }
            executing.await()
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive(); throw TransferRetry()
        } finally { executing.cancel() }
    }
}
