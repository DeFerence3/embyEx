package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

/** The root UI owns this job. Stop network work when backgrounded; OS download jobs are separate. */
@Single
class OfflineReconnection(private val session: OfflineUiSession, private val environment: DownloadEnvironment,
    private val connector: OfflineServerConnector, private val reconciler: OfflineReconciler,
    private val repository: OfflineRepository, private val controller: DownloadController) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run(foreground: StateFlow<Boolean>, pause: suspend (Long) -> Unit = { delay(it) }) {
        combine(foreground, session.account, environment.connection) { visible, account, connection ->
            if (visible && account?.key == connection?.let { OfflineAccountKey(it.serverId, it.userId) }) connection else null
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
                } catch (_: Exception) { /* Storage/service failure: retain data and retry at a bounded rate. */ }
                pause(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(60_000)
            }
        }
    }
}
