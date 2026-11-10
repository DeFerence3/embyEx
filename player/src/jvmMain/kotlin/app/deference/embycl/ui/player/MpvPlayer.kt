package app.deference.embycl.ui.player

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.openani.mediamp.mpv.JvmMpvMediampPlayer
import org.openani.mediamp.mpv.MPVHandle
import kotlin.coroutines.CoroutineContext

/** Playback and its raster cache belong to the original composition; windows borrow views. */
internal class MpvPlayer(
    coroutineContext: CoroutineContext,
    dispatcher: CoroutineDispatcher,
) : JvmMpvMediampPlayer(
    context = Unit,
    parentCoroutineContext = coroutineContext,
    mainDispatcher = dispatcher,
    configureOptions = { handle ->
        handle.option(key = "terminal", value = "no")
        handle.option(key = "msg-level", value = "all=no")
    },
) {
    private val mutableFrame = MutableStateFlow(value = 0L)
    val frame = mutableFrame.asStateFlow()
    private var renderer: MediampReadback? = null

    init {
        try {
            renderer = MediampReadback(
                impl as MPVHandle,
                { mutableFrame.update { it + 1L } },
            )
        } catch (failure: Throwable) {
            close()
            throw failure
        }
    }

    fun surface(): MediampReadback.Surface = requireNotNull(value = renderer).surface()

    /**
     * MediaMP 0.5.3's mpv backend has no PlaybackSpeed feature, so speed goes straight to mpv.
     * Returns false when mpv rejected the value.
     */
    fun setSpeed(
        speed: Float,
    ): Boolean = (impl as MPVHandle).setPropertyDouble("speed", speed.toDouble())

    /** Sets an mpv property by its string form; returns false when mpv rejected it. */
    fun setProperty(
        name: String,
        value: String,
    ): Boolean = (impl as MPVHandle).setPropertyString(name, value)

    override fun prepareNativeTeardown() {
        renderer?.close()
    }
}
