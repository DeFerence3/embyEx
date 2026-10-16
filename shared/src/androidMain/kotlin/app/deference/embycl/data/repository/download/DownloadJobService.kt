package app.deference.embycl.data.repository.download

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools

@RequiresApi(34)
class DownloadJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running: Job? = null
    override fun onStartJob(params: JobParameters): Boolean {
        setNotification(params, DownloadNotifications.ID, DownloadNotifications.build(this), JOB_END_NOTIFICATION_POLICY_REMOVE)
        val previous = running
        running = scope.launch {
            previous?.cancelAndJoin()
            val queue = KoinPlatformTools.defaultContext().get().get<DownloadQueue>()
            val notifications = launch {
                queue.activity.collect { setNotification(params, DownloadNotifications.ID, DownloadNotifications.build(this@DownloadJobService, it), JOB_END_NOTIFICATION_POLICY_REMOVE) }
            }
            try {
                val result = withContext(Dispatchers.IO) { queue.run() }
                jobFinished(params, result.retryAtEpochMillis != null)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { jobFinished(params, true) }
            finally { notifications.cancel() }
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean {
        running?.cancel()
        // Do not turn OS stops into user pauses. C6 recovers even if the process is killed.
        return params.stopReason != JobParameters.STOP_REASON_USER
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
