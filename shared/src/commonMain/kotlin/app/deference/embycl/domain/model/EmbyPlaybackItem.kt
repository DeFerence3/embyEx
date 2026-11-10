package app.deference.embycl.domain.model

data class EmbyPlaybackItem(
    val id: String,
    val title: String,
    val url: String,
    val positionMs: Long,
    /** Backdrop (or poster) shown behind the system media controls; null when the item has none. */
    val artworkUrl: String? = null,
    /** Episodes only: "S2 · E5". Null for films and other videos. */
    val episodeLabel: String? = null,
    /** Episodes only: the series name, shown above the title. */
    val seriesName: String? = null,
    val durationMs: Long? = null,
) {
    override fun toString(): String = "EmbyPlaybackItem(id=$id)"
}
