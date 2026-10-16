package app.deference.embycl.data.repository.download

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import app.deference.embycl.domain.model.download.OfflineAccountKey
import app.deference.embycl.domain.repository.DownloadQueueActivity
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools

internal object DownloadNotifications {
    const val ID = 0x454D43
    private const val CHANNEL = "offline_downloads"
    fun build(context: Context, activity: DownloadQueueActivity? = null): Notification {
        if (Build.VERSION.SDK_INT >= 26) context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "Media downloads", NotificationManager.IMPORTANCE_LOW))
        val row = activity?.download
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(row?.media?.item?.name ?: "Media downloads")
            .setContentText(if (row?.isLocallyAvailable == true) "Saving artwork and subtitles" else "Downloading for offline playback")
            .setOnlyAlertOnce(true).setOngoing(true).setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            builder.setContentIntent(PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        val total = row?.task?.progress?.totalBytes
        val progress = if (total != null && total > 0) ((row.task.progress.downloadedBytes.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0
        builder.setProgress(100, progress, total == null)
        if (row != null) {
            val intent = Intent(context, DownloadPauseReceiver::class.java).apply {
                putExtra("server", row.task.key.serverId); putExtra("user", row.task.key.userId)
            }
            val pause = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause downloads", pause)
        }
        return builder.build()
    }
    fun foreground(context: Context, activity: DownloadQueueActivity? = null): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(ID, build(context, activity), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(ID, build(context, activity))
}

/** Explicit, unexported notification action. Persist user pauses before ending execution. */
class DownloadPauseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val server = intent.getStringExtra("server")?.takeIf { it.isNotBlank() } ?: return
        val user = intent.getStringExtra("user")?.takeIf { it.isNotBlank() } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { KoinPlatformTools.defaultContext().get().get<DownloadQueue>().pauseAll(OfflineAccountKey(server, user)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { android.util.Log.e("Downloads", "Could not persist the notification pause action.") }
            finally { pending.finish() }
        }
    }
}
