package app.deference.embycl.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/** Owns focus and headphone-disconnect handling only while this player is playing. */
internal class AndroidMpvAudioFocus(
    private val context: Context,
    private val onPause: () -> Unit,
) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var registered = false
    private var granted = false
    private val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            // Video stays paused until the user explicitly resumes.
            handler.post { if (granted) onPause() }
        }
    }
    private val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(listener, handler)
            .build()
    } else null
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onPause()
        }
    }

    @Suppress("DEPRECATION")
    fun acquire(): Boolean {
        if (granted) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
            manager.requestAudioFocus(request)
        } else {
            manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted && !registered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(noisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(noisyReceiver, filter)
            }
            registered = true
        }
        return granted
    }

    @Suppress("DEPRECATION")
    fun release() {
        if (registered) {
            context.unregisterReceiver(noisyReceiver)
            registered = false
        }
        if (!granted) return
        granted = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
            manager.abandonAudioFocusRequest(request)
        } else {
            manager.abandonAudioFocus(listener)
        }
    }
}
