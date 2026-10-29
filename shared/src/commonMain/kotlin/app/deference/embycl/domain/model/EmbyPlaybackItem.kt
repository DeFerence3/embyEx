package app.deference.embycl.domain.model

data class EmbyPlaybackItem(
    val id: String,
    val title: String,
    val url: String,
    val positionMs: Long,
) {
    override fun toString(): String = "EmbyPlaybackItem(id=$id)"
}
