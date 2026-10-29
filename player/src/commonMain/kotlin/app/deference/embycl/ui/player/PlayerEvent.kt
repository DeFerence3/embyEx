package app.deference.embycl.ui.player

/** Terminal outcomes also remain in [PlayerState]; errors never expose source URLs or headers. */
internal sealed interface PlayerEvent {
    data class MediaEnded(
        val finalPositionMillis: Long,
        val durationMillis: Long?,
    ) : PlayerEvent

    data class SeekCompleted(
        val positionMillis: Long,
    ) : PlayerEvent

    data object ErrorOccurred : PlayerEvent
}
