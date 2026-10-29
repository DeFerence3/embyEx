package app.deference.embycl.domain.model

data class EmbyPlaybackRequest(
    val items: List<EmbyPlaybackItem>,
    val selectedIndex: Int,
    val serverUrl: String,
    val userId: String,
    val accessToken: String,
    val sessionId: String,
) {
    val item: EmbyPlaybackItem get() = items[selectedIndex]

    override fun toString(): String = "EmbyPlaybackRequest(sessionId=$sessionId)"
}
