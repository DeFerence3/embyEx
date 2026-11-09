package app.deference.embycl.ui.player

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import app.deference.embycl.core.utils.Log

private const val LOG_TAG = "PlayerMediaService"
private const val NOTIFICATION_ID = 1

internal class PlayerMediaService : Service() {
    private lateinit var notifications: AndroidMediaNotifications
    private var controller: MediaController? = null
    private var shown: ShownNotification? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(
            state: PlaybackState?,
        ) = refresh(force = false)

        override fun onMetadataChanged(
            metadata: MediaMetadata?,
        ) = refresh(force = false)

        override fun onSessionDestroyed() = stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        notifications = AndroidMediaNotifications(context = this)
    }

    override fun onBind(
        intent: Intent?,
    ): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val token = AndroidMediaSessionHost.current?.token
        if (token == null) {
            // A start request must still be answered with a foreground notification.
            enterForeground(notification = notifications.build(title = "", playing = false, artwork = null, token = null))
            stopSelf()
        } else {
            attach(token = token)
            intent?.action?.let(::relay)
            refresh(force = true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        controller?.unregisterCallback(controllerCallback)
        controller = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun attach(
        token: MediaSession.Token,
    ) {
        if (controller?.sessionToken == token) return
        controller?.unregisterCallback(controllerCallback)
        controller = MediaController(this, token).also { it.registerCallback(controllerCallback) }
        shown = null
    }

    private fun relay(
        action: String,
    ) {
        val controls = controller?.transportControls ?: return
        when (action) {
            ACTION_PLAY -> controls.play()
            ACTION_PAUSE -> controls.pause()
            ACTION_REWIND -> controls.rewind()
            ACTION_FAST_FORWARD -> controls.fastForward()
        }
    }

    private fun refresh(
        force: Boolean,
    ) {
        val active = controller ?: return
        val metadata = active.metadata
        val state = active.playbackState?.state
        val next = ShownNotification(
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING,
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
        )
        if (!force && next == shown) return
        shown = next
        enterForeground(
            notification = notifications.build(
                title = next.title,
                playing = next.playing,
                artwork = next.artwork,
                token = active.sessionToken,
            ),
        )
    }

    private fun enterForeground(
        notification: Notification,
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (exception: IllegalStateException) {
            Log.w(tag = LOG_TAG) { "Could not enter the foreground: ${exception::class.simpleName}" }
            stopSelf()
        } catch (exception: SecurityException) {
            Log.w(tag = LOG_TAG) { "Foreground service not permitted: ${exception::class.simpleName}" }
            stopSelf()
        }
    }

    private data class ShownNotification(
        val title: String,
        val playing: Boolean,
        val artwork: Bitmap?,
    )

    companion object {
        const val ACTION_PLAY = "app.deference.embycl.player.action.PLAY"
        const val ACTION_PAUSE = "app.deference.embycl.player.action.PAUSE"
        const val ACTION_REWIND = "app.deference.embycl.player.action.REWIND"
        const val ACTION_FAST_FORWARD = "app.deference.embycl.player.action.FAST_FORWARD"
    }
}
