package app.deference.embycl.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.os.Build
import androidx.annotation.RequiresApi
import app.deference.embycl.player.R

private const val CHANNEL_ID = "playback"

internal class AndroidMediaNotifications(
    private val context: Context,
) {
    private val contentIntent: PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    private val rewind = action(
        icon = android.R.drawable.ic_media_rew,
        label = R.string.media_notification_rewind,
        action = PlayerMediaService.ACTION_REWIND,
    )
    private val play = action(
        icon = android.R.drawable.ic_media_play,
        label = R.string.media_notification_play,
        action = PlayerMediaService.ACTION_PLAY,
    )
    private val pause = action(
        icon = android.R.drawable.ic_media_pause,
        label = R.string.media_notification_pause,
        action = PlayerMediaService.ACTION_PAUSE,
    )
    private val forward = action(
        icon = android.R.drawable.ic_media_ff,
        label = R.string.media_notification_forward,
        action = PlayerMediaService.ACTION_FAST_FORWARD,
    )

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel()
    }

    fun build(
        title: String,
        playing: Boolean,
        artwork: Bitmap?,
        token: MediaSession.Token?,
    ): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_LOW)
        }
        val style = Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2)
        token?.let { style.setMediaSession(it) }
        contentIntent?.let { builder.setContentIntent(it) }
        artwork?.let { builder.setLargeIcon(it) }
        return builder
            .setSmallIcon(R.drawable.ic_media_notification)
            .setContentTitle(title)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .addAction(rewind)
            .addAction(if (playing) pause else play)
            .addAction(forward)
            .setStyle(style)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.media_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.media_notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun action(
        icon: Int,
        label: Int,
        action: String,
    ): Notification.Action = Notification.Action.Builder(
        Icon.createWithResource(context, icon),
        context.getString(label),
        PendingIntent.getService(
            context,
            0,
            Intent(context, PlayerMediaService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ),
    ).build()
}
