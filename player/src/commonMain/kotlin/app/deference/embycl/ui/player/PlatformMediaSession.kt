package app.deference.embycl.ui.player

internal interface PlatformMediaSession {
    fun update(
        snapshot: MediaSessionSnapshot,
        seeked: Boolean,
    )
    fun release()
}
