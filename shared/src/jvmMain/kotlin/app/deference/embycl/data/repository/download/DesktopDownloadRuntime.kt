package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single(binds = [DownloadEnvironment::class])
class DesktopDownloadEnvironment : SessionDownloadEnvironment() {
    // No portable JVM metering API. A strict unmetered policy blocks unknown networks.
    override val network = MutableStateFlow(DownloadNetwork(available = true)).asStateFlow()
}

/** Application-owned, independent of any screen. A stopped application resumes via disk recovery. */
@Single(binds = [DownloadScheduler::class])
class DesktopDownloadRuntime(private val queue: DownloadQueue, private val environment: DownloadEnvironment) : DownloadScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var runner: Job? = null
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()

    @Synchronized fun start() {
        if (runner != null) return
        runner = scope.launch {
            val changes = launch {
                combine(environment.connection, environment.network, environment.unmeteredOnly) { _, _, _ -> Unit }
                    .collect { wake.trySend(Unit) }
            }
            try {
                wake.trySend(Unit)
                while (isActive) {
                    wake.receive()
                    val retry = try { queue.run().also { _failed.value = it.storageFailure != null }.retryAtEpochMillis }
                    catch (e: CancellationException) { throw e }
                    catch (_: Exception) { _failed.value = true; Clock.System.now().toEpochMilliseconds() + 30_000 }
                    if (retry != null) {
                        val wait = (retry - Clock.System.now().toEpochMilliseconds()).coerceIn(1, 60_000)
                        withTimeoutOrNull(wait) { wake.receive() }
                        wake.trySend(Unit)
                    }
                }
            } finally { changes.cancel() }
        }
    }
    override suspend fun requestExecution(userInitiated: Boolean): DownloadScheduling {
        start()
        return if (wake.trySend(Unit).isSuccess) DownloadScheduling.SCHEDULED else DownloadScheduling.UNAVAILABLE
    }
    suspend fun close() { scope.coroutineContext.job.cancelAndJoin(); wake.close() }
}
