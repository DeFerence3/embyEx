package app.deference.embycl.ui.player

/**
 * The queue as the overlay uses it. [autoplay] and [cancelled] are owned by the player content, because the
 * end-of-episode decision is made there; [onNavigate] closes this episode and asks the host for another.
 */
internal class PlayerQueueControls(
    val queue: PlayerQueue,
    val autoplay: Boolean,
    val onAutoplayChange: (Boolean) -> Unit,
    val cancelled: Boolean,
    val onCancelUpNext: () -> Unit,
    val onNavigate: (PlayerQueueEntry) -> Unit,
)
