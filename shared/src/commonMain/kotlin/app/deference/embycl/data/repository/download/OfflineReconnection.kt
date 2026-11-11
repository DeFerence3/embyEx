package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.domain.model.download.OfflineAccountKey
import app.deference.embycl.domain.repository.DownloadEnvironment
import app.deference.embycl.domain.repository.OfflineRepository
import app.deference.embycl.domain.repository.OfflineServerConnector
import app.deference.embycl.domain.repository.OfflineServerResult
import app.deference.embycl.domain.repository.OfflineSyncException
import app.deference.embycl.domain.repository.OfflineSyncFailure
import app.deference.embycl.domain.repository.OfflineUiSession
import app.deference.embycl.domain.repository.ServerReconnector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

@Single(binds = [ServerReconnector::class])
class OfflineReconnection(
    private val session: OfflineUiSession,
    private val environment: DownloadEnvironment,
    private val connector: OfflineServerConnector,
    private val reconciler: OfflineReconciler,
    private val repository: OfflineRepository,
    private val controller: DownloadController
) : ServerReconnector {
    private val wake = Channel<Unit>(Channel.CONFLATED)

    override fun reconnectNow() { wake.trySend(Unit) }

    suspend fun run(foreground: StateFlow<Boolean>) {
        combine(
            foreground,
            session.account,
            environment.connection,
            environment.network
        ) { visible, account, connection, network ->
            if (visible && network.available && account?.key == connection?.let { OfflineAccountKey(it.serverId, it.userId) }) connection else null
        }.collectLatest { connection ->
            if (connection == null) return@collectLatest
            val account = OfflineAccountKey(connection.serverId, connection.userId)
            var delayMs = 5_000L
            var reconnected = false
            while (currentCoroutineContext().isActive) {
                try {
                    // Generic Internet validation is deliberately not a prerequisite for a LAN probe.
                    val result = connector.verify(account, connection.baseUrl, connection)
                    if (environment.connection.value !== connection || session.account.value?.key != account) return@collectLatest
                    when (result) {
                        OfflineServerResult.CONNECTED -> {
                            session.setConnected(true)
                            if (!reconnected) {
                                val rows = repository.observeDownloads(account).first()
                                rows.filter { it.task.status == DownloadStatus.WAITING_FOR_AUTHENTICATION }.forEach { controller.queue.retryAuthentication(it.task.key) }
                                if (rows.any { it.task.status in setOf(DownloadStatus.QUEUED, DownloadStatus.WAITING_FOR_NETWORK, DownloadStatus.WAITING_FOR_AUTHENTICATION) })
                                    controller.serverReconnected()
                                reconnected = true
                            }
                            reconciler.reconcile(connection)
                            delayMs = 60_000
                        }
                        OfflineServerResult.SIGN_IN_REQUIRED -> { session.expire(connection); session.setConnected(false); return@collectLatest }
                        else -> { session.setConnected(false); reconnected = false }
                    }
                } catch (e: CancellationException) { throw e }
                catch (e: OfflineSyncException) {
                    if (environment.connection.value !== connection) return@collectLatest
                    if (e.reason == OfflineSyncFailure.AUTHENTICATION) { session.expire(connection); session.setConnected(false); return@collectLatest }
                    session.setConnected(false); reconnected = false
                } catch ( _: Exception) {

                }
                val woken = withTimeoutOrNull(delayMs.milliseconds) { wake.receive() } != null
                delayMs = if (woken) 5_000 else (delayMs * 2).coerceAtMost(60_000)
            }
        }
    }
}
