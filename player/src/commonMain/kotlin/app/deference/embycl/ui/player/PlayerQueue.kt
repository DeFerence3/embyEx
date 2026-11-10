package app.deference.embycl.ui.player

/**
 * The episodes around the playing one. The host owns the queue; the player only shows it and asks the host to
 * move through `EmbeddedPlayerScreen`'s `onNavigate`.
 *
 * @param upcoming episodes after the playing one, in play order; the first is "Up next".
 * @param autoplay whether the next episode starts by itself when this one ends.
 */
data class PlayerQueue(
    val previous: PlayerQueueEntry?,
    val upcoming: List<PlayerQueueEntry>,
    val autoplay: Boolean,
    val onAutoplayChange: (Boolean) -> Unit,
) {
    val next: PlayerQueueEntry? get() = upcoming.firstOrNull()
}
