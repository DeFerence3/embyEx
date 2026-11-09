package app.deference.embycl.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.deference.embycl.core.utils.Log
import kotlin.concurrent.thread

private const val SESSION_TAG = "EmbyEx playback"
private const val LOG_TAG = "AndroidMediaSession"
private const val SKIP_MILLIS = 10_000L

private const val ACTIONS = PlaybackState.ACTION_PLAY or
    PlaybackState.ACTION_PAUSE or
    PlaybackState.ACTION_PLAY_PAUSE or
    PlaybackState.ACTION_STOP or
    PlaybackState.ACTION_SEEK_TO or
    PlaybackState.ACTION_REWIND or
    PlaybackState.ACTION_FAST_FORWARD

internal class AndroidMediaSession(
    context: Context,
    source: PlayerSource,
    private val player: EmbeddedPlayer,
    private val dispatchAction: (() -> Unit) -> Unit,
    private val stopPlayback: () -> Unit,
) : PlatformMediaSession {
    private val appContext = context.applicationContext
    private val serviceIntent = Intent(appContext, PlayerMediaService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val session = MediaSession(appContext, SESSION_TAG)
    private var started = false
    private var released = false
    private var artwork: Bitmap? = null
    private var lastSnapshot: MediaSessionSnapshot? = null
    private var publishedMetadata: Triple<String, Long?, Bitmap?>? = null

    val token: MediaSession.Token get() = session.sessionToken

    private val callback = object : MediaSession.Callback() {
        override fun onPlay() = dispatchAction { player.play() }

        override fun onPause() = dispatchAction { player.pause() }

        override fun onStop() = dispatchAction { stopPlayback() }

        override fun onSeekTo(
            pos: Long,
        ) = dispatchAction { player.seekTo(positionMillis = pos) }

        override fun onFastForward() = dispatchAction { player.skip(deltaMillis = SKIP_MILLIS) }

        override fun onRewind() = dispatchAction { player.skip(deltaMillis = -SKIP_MILLIS) }
    }

    init {
        session.setCallback(callback, mainHandler)
        handleMediaButtonsOnLegacyApis()
        source.artworkUri?.let { uri -> loadArtwork(uri = uri, headers = source.headers) }
    }

    override fun update(
        snapshot: MediaSessionSnapshot,
        seeked: Boolean,
    ) {
        if (released) return
        lastSnapshot = snapshot
        publishMetadata(snapshot = snapshot)
        session.setPlaybackState(snapshot.toPlaybackState())
        if (!started && snapshot.status != MediaSessionStatus.Stopped) start()
    }

    override fun release() {
        if (released) return
        released = true
        session.isActive = false
        session.release()
        // A newer player may already own the service.
        if (AndroidMediaSessionHost.current === this) {
            AndroidMediaSessionHost.current = null
            appContext.stopService(serviceIntent)
        }
    }

    private fun start() {
        started = true
        session.isActive = true
        AndroidMediaSessionHost.current = this
        try {
            ContextCompat.startForegroundService(appContext, serviceIntent)
        } catch (exception: IllegalStateException) {
            Log.w(tag = LOG_TAG) { "Playback notification not started: ${exception::class.simpleName}" }
        } catch (exception: SecurityException) {
            Log.w(tag = LOG_TAG) { "Playback notification not allowed: ${exception::class.simpleName}" }
        }
    }

    private fun publishMetadata(
        snapshot: MediaSessionSnapshot,
    ) {
        val next = Triple(snapshot.title, snapshot.durationMillis, artwork)
        if (next == publishedMetadata) return
        publishedMetadata = next
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, snapshot.title)
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, snapshot.title)
        snapshot.durationMillis?.let { metadata.putLong(MediaMetadata.METADATA_KEY_DURATION, it) }
        artwork?.let { metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, it) }
        session.setMetadata(metadata.build())
    }

    private fun loadArtwork(
        uri: String,
        headers: Map<String, String>,
    ) {
        thread(isDaemon = true, name = "embyex-media-artwork") {
            val bitmap = try {
                loadMediaArtwork(context = appContext, uri = uri, headers = headers)
            } catch (exception: Exception) {
                Log.w(tag = LOG_TAG) { "Media artwork unavailable: ${exception::class.simpleName}" }
                null
            } ?: return@thread
            mainHandler.post {
                if (released) return@post
                artwork = bitmap
                lastSnapshot?.let { publishMetadata(snapshot = it) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun handleMediaButtonsOnLegacyApis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
        }
    }
}

private fun MediaSessionSnapshot.toPlaybackState(): PlaybackState {
    val state = when (status) {
        MediaSessionStatus.Buffering -> PlaybackState.STATE_BUFFERING
        MediaSessionStatus.Playing -> PlaybackState.STATE_PLAYING
        MediaSessionStatus.Paused -> PlaybackState.STATE_PAUSED
        MediaSessionStatus.Stopped -> PlaybackState.STATE_STOPPED
    }
    return PlaybackState.Builder()
        .setActions(ACTIONS)
        .setState(
            state,
            positionMillis,
            if (status == MediaSessionStatus.Playing) speed else 0f,
            SystemClock.elapsedRealtime(),
        )
        .build()
}
