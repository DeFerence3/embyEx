package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-owned playback boundary. Commands and native notifications run on the host UI dispatcher.
 * Each instance opens one source; its composition owns the lifetime and calls [close].
 */
internal interface EmbeddedPlayer {
    val state: StateFlow<PlayerState>
    val currentPositionMillis: StateFlow<Long>
    val mediaProperties: StateFlow<PlayerMediaProperties?>
    /** One screen collector consumes these facts. Subscribe before calling [open]. */
    val events: Flow<PlayerEvent>
    val audioTracks: PlayerTrackGroup?
    val subtitleTracks: PlayerTrackGroup?
    val playbackSpeed: PlayerSpeed?
    val aspectRatio: PlayerAspect?

    /** Returns when the source is ready. Cancellation releases the in-flight source. */
    suspend fun open(
        source: PlayerSource,
        playWhenReady: Boolean,
        startPositionMillis: Long,
    )

    fun play()
    fun pause()
    fun seekTo(
        positionMillis: Long,
    )

    fun skip(
        deltaMillis: Long,
    ) = seekTo(positionMillis = (currentPositionMillis.value + deltaMillis).coerceAtLeast(minimumValue = 0))

    fun togglePlayWhenReady() {
        if (state.value.playWhenReady) pause() else play()
    }

    /** Idempotent; native teardown may finish asynchronously in the owning scope. */
    fun close()
}
