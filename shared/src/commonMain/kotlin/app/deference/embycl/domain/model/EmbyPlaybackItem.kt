package app.deference.embycl.domain.model

data class EmbyPlaybackItem(
    val id: String,
    val title: String,
    val url: String,
    val positionMs: Long,
    /** Backdrop (or poster) shown behind the system media controls; null when the item has none. */
    val artworkUrl: String? = null,
) {
    override fun toString(): String = "EmbyPlaybackItem(id=$id)"
}
