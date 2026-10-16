package app.deference.embycl.data.repository.download

import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Clock

/** One application instance, one OS queue lease, and one active original/asset transfer. */
@Single
class DownloadQueue(
    private val repository: OfflineRepository,
    private val files: OfflineFileStore,
    private val storage: OfflineStorage,
    private val transfer: DownloadTransferEngine,
    private val assets: DownloadAssetFetcher,
    private val environment: DownloadEnvironment,
) {
    private val execution = Mutex()
    private val controls = Mutex()
    private var active: Deferred<Unit>? = null
    private var activeKey: DownloadKey? = null
    private val _activity = MutableStateFlow<DownloadQueueActivity?>(null)
    val activity = _activity.asStateFlow()

    /** Startup reconciliation without scheduling any network work. */
    suspend fun recover(): DownloadQueueResult {
        if (!execution.tryLock()) return DownloadQueueResult(now() + RETRY_FLOOR, OfflineStorageFailure.BUSY)
        try {
            return files.withQueueLease {
                val issues = controls.withLock { storage.recoverAll() }
                DownloadQueueResult(if (issues.isEmpty()) null else now() + RETRY_FLOOR, issues.firstOrNull()?.reason)
            }
        } catch (e: OfflineStorageException) { return DownloadQueueResult(now() + RETRY_FLOOR, e.reason) }
        finally { execution.unlock() }
    }

    suspend fun enqueue(account: OfflineAccount, ready: DownloadResolution.Ready): StoredDownload = controls.withLock {
        val connection = environment.connection.value
        require(connection != null && account.key == OfflineAccountKey(connection.serverId, connection.userId))
        require(ready.source.key.accountKey() == account.key)
        repository.enqueue(account, ready)
    }

    /** Cancel/join first, then mutate under the file lease. A completed finalization wins a racing pause. */
    suspend fun pause(key: DownloadKey) = control(key) { row ->
        if (row.task.status in pausable) row.task.pause() else row.task
    }
    suspend fun retry(key: DownloadKey) = control(key) { row ->
        if (row.task.status in retryable) row.task.retry() else row.task
    }
    suspend fun retryAuthentication(key: DownloadKey) = control(key) { row ->
        if (row.task.status == DownloadStatus.WAITING_FOR_AUTHENTICATION) row.task.retry() else row.task
    }
    suspend fun retryAssets(key: DownloadKey) = controls.withLock {
        stopActive(key)
        val row = repository.getDownload(key) ?: return@withLock
        if (!row.isLocallyAvailable) return@withLock
        resetFailedAssets(row)
    }
    private suspend fun resetFailedAssets(row: StoredDownload) {
        val key = row.task.key
        files.withDownload(row.id) {
            repository.observeAssets(key).first().filter { it.state == OfflineAssetState.FAILED || it.state == OfflineAssetState.MISSING }.forEach {
                repository.saveAsset(key, row.version, it.copy(state = OfflineAssetState.PENDING, relativePath = null, sizeBytes = null))
            }
        }
    }
    suspend fun cancel(key: DownloadKey): StoredDownload? {
        return controls.withLock {
            stopActive(key)
            val saved = mutate(key) { if (it.task.status != DownloadStatus.COMPLETED) it.task.cancel() else it.task }
            if (saved?.task?.status == DownloadStatus.CANCELED) storage.recover(key) else saved
        }
    }
    suspend fun remove(key: DownloadKey): Boolean = controls.withLock {
        stopActive(key)
        val row = repository.getDownload(key) ?: return@withLock false
        storage.remove(key, row.version)
    }
    suspend fun pauseAll(account: OfflineAccountKey) = controls.withLock {
        if (activeKey?.accountKey() == account) active?.cancelAndJoin()
        repository.observeDownloads(account).first().forEach { row ->
            mutate(row.task.key) { if (it.task.status in pausable) it.task.pause() else it.task }
        }
    }
    private suspend fun control(key: DownloadKey, change: (StoredDownload) -> DownloadTask): StoredDownload? = controls.withLock {
        stopActive(key)
        mutate(key, change)
    }
    private suspend fun stopActive(key: DownloadKey) { if (activeKey == key) active?.cancelAndJoin() }
    private suspend fun mutate(key: DownloadKey, change: (StoredDownload) -> DownloadTask): StoredDownload? {
        val before = repository.getDownload(key) ?: return null
        return files.withDownload(before.id) {
            val row = repository.getDownload(key)?.takeIf { it.id == before.id } ?: return@withDownload null
            val task = change(row)
            if (task == row.task) row else repository.updateDownload(key, row.version) { it.change(task = task) }
        }
    }

    /** Called by a platform-owned job. Returns when idle or awaiting a later platform retry. */
    suspend fun run(): DownloadQueueResult {
        if (!execution.tryLock()) return DownloadQueueResult(now() + RETRY_FLOOR)
        try {
            return files.withQueueLease {
                val issues = controls.withLock { storage.recoverAll() }
                val result = drain()
                if (issues.isEmpty()) result else result.copy(
                    retryAtEpochMillis = minOf(result.retryAtEpochMillis ?: Long.MAX_VALUE, now() + RETRY_FLOOR),
                    storageFailure = issues.first().reason)
            }
        } catch (e: OfflineStorageException) {
            return DownloadQueueResult(now() + RETRY_FLOOR, e.reason)
        } finally { execution.unlock() }
    }

    private suspend fun drain(): DownloadQueueResult = supervisorScope {
        val attemptedAssets = mutableSetOf<String>()
        val attemptedMedia = mutableSetOf<String>()
        while (true) {
            currentCoroutineContext().ensureActive()
            val connection = environment.connection.value ?: return@supervisorScope DownloadQueueResult()
            val account = OfflineAccountKey(connection.serverId, connection.userId)
            val hasWork = repository.observeDownloads(account).first().any { row ->
                row.task.status in setOf(DownloadStatus.QUEUED, DownloadStatus.WAITING_FOR_NETWORK) ||
                    (row.isLocallyAvailable && row.id !in attemptedAssets && assets.hasPending(row))
            }
            if (!hasWork) return@supervisorScope DownloadQueueResult()
            val network = environment.network.value
            val unmetered = environment.unmeteredOnly.first()
            if (!network.available || (unmetered && network.unmetered != true))
                return@supervisorScope DownloadQueueResult(now() + RETRY_FLOOR)
            var nextRetry: Long? = null
            val job = controls.withLock {
                if (environment.connection.value !== connection) return@withLock null
                val rows = repository.observeDownloads(account).first().sortedWith(compareBy({ it.createdAt }, { it.id }))
                var selected: StoredDownload? = null
                for (row in rows) {
                    when (row.task.status) {
                        DownloadStatus.QUEUED -> {
                            if (row.id in attemptedMedia) { nextRetry = minOf(nextRetry ?: Long.MAX_VALUE, now() + RETRY_FLOOR); continue }
                            selected = row; attemptedMedia += row.id; break
                        }
                        DownloadStatus.WAITING_FOR_NETWORK -> {
                            val due = row.task.retryAtEpochMillis ?: 0
                            if (due <= now()) {
                                selected = mutate(row.task.key) { it.task.retry() }
                                attemptedMedia += row.id
                                break
                            }
                            nextRetry = minOf(nextRetry ?: Long.MAX_VALUE, due)
                        }
                        DownloadStatus.COMPLETED -> if (row.isLocallyAvailable && row.id !in attemptedAssets && assets.hasPending(row)) {
                            selected = row; attemptedAssets += row.id; break
                        }
                        else -> Unit // Manual pauses, authentication waits, failures and cancellations stay put.
                    }
                }
                val row = selected ?: return@withLock null
                activeKey = row.task.key
                async(start = CoroutineStart.LAZY) {
                    coroutineScope {
                        val observation = launch {
                            repository.observeDownloads(account).collect { records ->
                                _activity.value = records.firstOrNull { it.id == row.id }?.let(::DownloadQueueActivity)
                            }
                        }
                        try {
                            if (row.task.status == DownloadStatus.QUEUED) {
                                transfer.transfer(connection, row.task.key, row.version)
                                // Avoid tight retry loops across fresh engine invocations.
                                val after = repository.getDownload(row.task.key)
                                if (after?.task?.status == DownloadStatus.WAITING_FOR_NETWORK &&
                                    (after.task.retryAtEpochMillis ?: 0) < now() + RETRY_FLOOR) {
                                    mutate(row.task.key) { it.task.retry().waitForNetwork(now() + RETRY_FLOOR) }
                                }
                            }
                            val complete = repository.getDownload(row.task.key)
                            if (complete?.isLocallyAvailable == true && environment.connection.value === connection) {
                                attemptedAssets += row.id
                                // A newly completed/replaced original gets one fresh optional-asset
                                // attempt, including assets invalidated by C7's source-change check.
                                if (row.task.status == DownloadStatus.QUEUED) resetFailedAssets(complete)
                                assets.fetch(connection, complete)
                            }
                        } finally { observation.cancelAndJoin() }
                    }
                }.also { active = it; it.start() }
            }
            if (job == null) {
                if (environment.connection.value !== connection) continue
                return@supervisorScope DownloadQueueResult(nextRetry)
            }
            // Any connection snapshot change (including URL/token), sign-out or network-policy
            // change interrupts the old operation; the next iteration reads a fresh snapshot.
            val guard = launch(start = CoroutineStart.UNDISPATCHED) {
                combine(environment.connection, environment.network, environment.unmeteredOnly) { c, n, u ->
                    c !== connection || !n.available || (u && n.unmetered != true)
                }.first { it }
                job.cancel()
            }
            try { job.await() }
            catch (_: CancellationException) { currentCoroutineContext().ensureActive() }
            finally {
                withContext(NonCancellable) {
                    job.cancelAndJoin(); guard.cancelAndJoin()
                    controls.withLock {
                        if (active === job) { active = null; activeKey = null; _activity.value = null }
                    }
                }
            }
            // Recover an interrupted writer before considering another start; preserves a user pause.
            controls.withLock { storage.recoverAll() }
        }
        @Suppress("UNREACHABLE_CODE") DownloadQueueResult()
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
    private companion object {
        const val RETRY_FLOOR = 30_000L
        val pausable = setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.WAITING_FOR_NETWORK, DownloadStatus.WAITING_FOR_AUTHENTICATION)
        val retryable = setOf(DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.WAITING_FOR_NETWORK, DownloadStatus.WAITING_FOR_AUTHENTICATION)
    }
}
