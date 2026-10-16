package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import org.koin.core.annotation.Single

/** C9's entry point: persist intent before requesting platform execution. */
@Single
class DownloadController(
    val queue: DownloadQueue,
    private val scheduler: DownloadScheduler,
    private val environment: DownloadEnvironment,
) {
    val unmeteredOnly get() = environment.unmeteredOnly
    suspend fun resumeQueue() = scheduler.requestExecution(userInitiated = true)
    suspend fun serverReconnected() = scheduler.requestExecution(userInitiated = false)
    suspend fun enqueue(account: OfflineAccount, ready: DownloadResolution.Ready): DownloadScheduling {
        queue.enqueue(account, ready)
        return scheduler.requestExecution(userInitiated = true)
    }
    suspend fun retry(key: DownloadKey): DownloadScheduling {
        queue.retry(key)
        return scheduler.requestExecution(userInitiated = true)
    }
    suspend fun retryAssets(key: DownloadKey): DownloadScheduling {
        queue.retryAssets(key)
        return scheduler.requestExecution(userInitiated = true)
    }
    suspend fun setUnmeteredOnly(value: Boolean): DownloadScheduling {
        environment.setUnmeteredOnly(value)
        return scheduler.requestExecution(userInitiated = true)
    }
}
