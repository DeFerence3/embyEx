package app.deference.embycl.data.repository.download

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools

class DownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = coroutineScope {
        // A pre-upgrade WorkRequest must not start an Android 14+ foreground fallback.
        if (Build.VERSION.SDK_INT >= 34) return@coroutineScope Result.success()
        val queue = KoinPlatformTools.defaultContext().get().get<DownloadQueue>()
        var notifications: Job? = null
        try {
            setForeground(DownloadNotifications.foreground(applicationContext))
            notifications = launch { queue.activity.collect { setForeground(DownloadNotifications.foreground(applicationContext, it)) } }
            val result = queue.run()
            if (result.retryAtEpochMillis != null) Result.retry() else Result.success()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.retry() }
        finally { notifications?.cancelAndJoin() }
    }
}
