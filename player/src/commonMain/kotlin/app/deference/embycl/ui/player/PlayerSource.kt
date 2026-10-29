package app.deference.embycl.ui.player

/** An in-memory source. Never log authenticated media URLs or headers. */
data class PlayerSource(
    val key: String,
    val title: String,
    val uri: String,
    val positionMs: Long,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<PlayerSubtitle> = emptyList(),
    // Called only for a user-requested handoff; hosts supply authentication or a shareable content URI.
    val resolveExternalUri: (() -> String)? = null,
) {
    override fun toString(): String = "PlayerSource(uri=<redacted>, headers=<redacted>)"
}
