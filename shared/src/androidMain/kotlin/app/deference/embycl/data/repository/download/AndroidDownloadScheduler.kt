package app.deference.embycl.data.repository.download

import android.app.Activity
import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import androidx.work.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.concurrent.TimeUnit

/** Construct during Application.onCreate, before the first Activity becomes visible. */
@Single(binds = [DownloadScheduler::class])
class AndroidDownloadScheduler(private val context: Context, private val environment: DownloadEnvironment) : DownloadScheduler {
    @Volatile private var visibleActivities = 0
    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { visibleActivities++ }
            override fun onActivityStopped(activity: Activity) { visibleActivities = (visibleActivities - 1).coerceAtLeast(0) }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override suspend fun requestExecution(userInitiated: Boolean): DownloadScheduling {
        if (!userInitiated || visibleActivities == 0) return DownloadScheduling.REQUIRES_USER_ACTION
        val unmetered = environment.unmeteredOnly.first()
        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                val scheduler = context.getSystemService(JobScheduler::class.java)
                // Replace even a pending/running job: keeping it can lose a new enqueue
                // racing with its final idle check. C6/C7 preserve an interrupted prefix.
                run {
                    val network = NetworkRequest.Builder().apply {
                        // Local networks are eligible; the transfer engine probes the actual server.
                        if (unmetered) addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    }.build()
                    val job = JobInfo.Builder(JOB_ID, ComponentName(context, DownloadJobService::class.java))
                        .setUserInitiated(true).setRequiredNetwork(network)
                        .setExtras(android.os.PersistableBundle().apply { putBoolean("unmetered", unmetered) })
                        .build()
                    if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) return DownloadScheduling.UNAVAILABLE
                }
            } else {
                // WorkManager CONNECTED checks Internet validation on some OS versions. Leave
                // that constraint unset; the shared queue checks transport + unmetered policy.
                val work = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
                withContext(Dispatchers.IO) {
                    WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, work).result.get()
                }
            }
            DownloadScheduling.SCHEDULED
        } catch (_: SecurityException) { DownloadScheduling.REQUIRES_USER_ACTION }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { DownloadScheduling.UNAVAILABLE }
    }
    companion object {
        const val JOB_ID = 0x454D42
        const val WORK_NAME = "offline-media-downloads"
    }
}
