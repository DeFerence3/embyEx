package app.deference.embycl.domain.model

/** Milliseconds from the beginning of the selected item; null means media never opened. */
data class PlaybackPosition(
    val positionMs: Long? = null,
    val durationMs: Long? = null,
    val paused: Boolean = true,
    val completed: Boolean = false,
)
