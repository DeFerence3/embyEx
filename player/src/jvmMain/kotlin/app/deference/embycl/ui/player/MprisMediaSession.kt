package app.deference.embycl.ui.player

import app.deference.embycl.core.utils.Log
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant
import java.util.concurrent.Executors

private const val LOG_TAG = "MprisMediaSession"
private const val BUS_NAME = "org.mpris.MediaPlayer2.embyex"
private const val PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"

internal class MprisMediaSession(
    source: PlayerSource,
    player: EmbeddedPlayer,
    dispatchAction: (() -> Unit) -> Unit,
    stopPlayback: () -> Unit,
) : PlatformMediaSession {
    private val exported = MprisObject(
        artworkUri = source.artworkUri,
        player = player,
        dispatchAction = dispatchAction,
        stopPlayback = stopPlayback,
    )
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "embyex-mpris").apply { isDaemon = true }
    }
    private var connection: DBusConnection? = null

    @Volatile
    private var released = false

    init {
        worker.execute { guarded(action = "connect", block = ::connect) }
    }

    override fun update(
        snapshot: MediaSessionSnapshot,
        seeked: Boolean,
    ) {
        if (released) return
        worker.execute { guarded(action = "publish") { publish(snapshot = snapshot, seeked = seeked) } }
    }

    override fun release() {
        if (released) return
        released = true
        worker.execute { guarded(action = "disconnect") { connection?.close() } }
        worker.shutdown()
    }

    private fun connect() {
        val bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()
        try {
            try {
                bus.requestBusName(BUS_NAME)
            } catch (_: DBusException) {
                bus.requestBusName("$BUS_NAME.instance${ProcessHandle.current().pid()}")
            }
            bus.exportObject(MPRIS_OBJECT_PATH, exported)
        } catch (failure: Exception) {
            bus.close()
            throw failure
        }
        connection = bus
    }

    private fun publish(
        snapshot: MediaSessionSnapshot,
        seeked: Boolean,
    ) {
        val bus = connection ?: return
        val previous = exported.snapshot
        exported.snapshot = snapshot
        val changed = mutableMapOf<String, Variant<*>>()
        if (previous?.status.toPlaybackStatus() != snapshot.status.toPlaybackStatus()) {
            changed["PlaybackStatus"] = exported.getPlaybackStatus()
        }
        if (previous?.title != snapshot.title || previous.durationMillis != snapshot.durationMillis) {
            changed["Metadata"] = exported.getMetadata()
        }
        if (previous?.speed != snapshot.speed) changed["Rate"] = exported.getRate()
        if (changed.isNotEmpty()) {
            bus.sendMessage(Properties.PropertiesChanged(MPRIS_OBJECT_PATH, PLAYER_INTERFACE, changed, mutableListOf()))
        }
        if (seeked) {
            bus.sendMessage(MprisPlayer.Seeked(MPRIS_OBJECT_PATH, snapshot.positionMillis * MICROS_PER_MILLI))
        }
    }

    private fun guarded(
        action: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (exception: Exception) {
            Log.w(tag = LOG_TAG) { "MPRIS $action failed: ${exception::class.simpleName}" }
        } catch (error: LinkageError) {
            Log.w(tag = LOG_TAG) { "MPRIS $action unavailable: ${error::class.simpleName}" }
        }
    }
}
