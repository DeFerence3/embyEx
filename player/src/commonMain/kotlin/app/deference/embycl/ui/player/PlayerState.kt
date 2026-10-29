package app.deference.embycl.ui.player

/** Playback intent stays separate from buffering so a stall does not toggle the play button. */
internal data class PlayerState(
    val mediaStatus: PlayerMediaStatus = PlayerMediaStatus.Idle,
    val playWhenReady: Boolean = false,
    val isBuffering: Boolean = false,
) {
    val isLoadingOrBuffering: Boolean
        get() = mediaStatus == PlayerMediaStatus.Opening || isBuffering
}
