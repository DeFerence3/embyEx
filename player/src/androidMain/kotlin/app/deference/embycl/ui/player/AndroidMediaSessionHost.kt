package app.deference.embycl.ui.player

internal object AndroidMediaSessionHost {
    @Volatile
    var current: AndroidMediaSession? = null
}
