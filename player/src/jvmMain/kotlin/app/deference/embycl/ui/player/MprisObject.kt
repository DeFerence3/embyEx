package app.deference.embycl.ui.player

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.types.Variant

internal const val MPRIS_OBJECT_PATH = "/org/mpris/MediaPlayer2"
internal const val MICROS_PER_MILLI = 1_000L
private const val TRACK_ID = "/app/deference/embycl/track/current"
private const val NO_TRACK_ID = "/org/mpris/MediaPlayer2/TrackList/NoTrack"
private const val MIN_RATE = 0.25
private const val MAX_RATE = 4.0

private val YES = Variant(true)
private val NO = Variant(false)
private val NO_STRINGS = Variant(emptyList<String>(), "as")

internal class MprisObject(
    val artworkUri: String?,
    private val player: EmbeddedPlayer,
    private val dispatchAction: (() -> Unit) -> Unit,
    private val stopPlayback: () -> Unit,
) : MprisRoot, MprisPlayer {
    @Volatile
    var snapshot: MediaSessionSnapshot? = null

    override fun getObjectPath(): String = MPRIS_OBJECT_PATH

    override fun raise() = Unit

    override fun quit() = Unit

    override fun getCanQuit(): Variant<*> = NO

    override fun getCanRaise(): Variant<*> = NO

    override fun getHasTrackList(): Variant<*> = NO

    override fun getIdentity(): Variant<*> = Variant("EmbyEx")

    override fun getDesktopEntry(): Variant<*> = Variant("embyex")

    override fun getSupportedUriSchemes(): Variant<*> = NO_STRINGS

    override fun getSupportedMimeTypes(): Variant<*> = NO_STRINGS

    override fun next() = Unit

    override fun previous() = Unit

    override fun pause() = dispatchAction { player.pause() }

    override fun playPause() = dispatchAction { player.togglePlayWhenReady() }

    override fun stop() = dispatchAction { stopPlayback() }

    override fun play() = dispatchAction { player.play() }

    override fun seek(
        offset: Long,
    ) = dispatchAction { player.skip(deltaMillis = offset / MICROS_PER_MILLI) }

    override fun setPosition(
        trackId: DBusPath,
        position: Long,
    ) {
        // A stale id addresses a track that is no longer playing.
        if (trackId.path == TRACK_ID) {
            dispatchAction { player.seekTo(positionMillis = position / MICROS_PER_MILLI) }
        }
    }

    override fun openUri(
        uri: String,
    ) = Unit

    override fun getPlaybackStatus(): Variant<*> = Variant(snapshot?.status.toPlaybackStatus())

    override fun getMetadata(): Variant<*> = Variant(metadataOf(snapshot = snapshot), "a{sv}")

    override fun getPosition(): Variant<*> = Variant(player.currentPositionMillis.value * MICROS_PER_MILLI)

    override fun getRate(): Variant<*> = Variant((snapshot?.speed ?: 1f).toDouble())

    override fun setRate(
        rate: Double,
    ) = dispatchAction { player.playbackSpeed?.set(speed = rate.coerceIn(MIN_RATE, MAX_RATE).toFloat()) }

    override fun getVolume(): Variant<*> = Variant(1.0)

    override fun getMinimumRate(): Variant<*> = Variant(MIN_RATE)

    override fun getMaximumRate(): Variant<*> = Variant(MAX_RATE)

    override fun getCanGoNext(): Variant<*> = NO

    override fun getCanGoPrevious(): Variant<*> = NO

    override fun getCanPlay(): Variant<*> = YES

    override fun getCanPause(): Variant<*> = YES

    override fun getCanSeek(): Variant<*> = YES

    override fun getCanControl(): Variant<*> = YES

    private fun metadataOf(
        snapshot: MediaSessionSnapshot?,
    ): MutableMap<String, Variant<*>> {
        if (snapshot == null) return mutableMapOf("mpris:trackid" to Variant(DBusPath(NO_TRACK_ID)))
        val metadata = mutableMapOf<String, Variant<*>>(
            "mpris:trackid" to Variant(DBusPath(TRACK_ID)),
            "xesam:title" to Variant(snapshot.title),
        )
        snapshot.durationMillis?.let { metadata["mpris:length"] = Variant(it * MICROS_PER_MILLI) }
        artworkUri?.let { metadata["mpris:artUrl"] = Variant(it) }
        return metadata
    }
}

internal fun MediaSessionStatus?.toPlaybackStatus(): String = when (this) {
    MediaSessionStatus.Playing, MediaSessionStatus.Buffering -> "Playing"
    MediaSessionStatus.Paused -> "Paused"
    MediaSessionStatus.Stopped, null -> "Stopped"
}
