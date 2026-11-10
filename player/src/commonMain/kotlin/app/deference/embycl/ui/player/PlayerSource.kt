package app.deference.embycl.ui.player

/** An in-memory source. Never log authenticated media URLs or headers. */
data class PlayerSource(
    val key: String,
    val title: String,
    /** Line shown above [title], for example "Northbound · S2 · E5"; null shows the title alone. */
    val subtitle: String? = null,
    val uri: String,
    val positionMs: Long,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<PlayerSubtitle> = emptyList(),
    // Called only for a user-requested handoff; hosts supply authentication or a shareable content URI.
    val resolveExternalUri: (() -> String)? = null,
    /** Image shown behind the system media controls, fetched with [headers]. */
    val artworkUri: String? = null,
    /** Episodes around this one; null for films and downloads, which hides previous, next and Up next. */
    val queue: PlayerQueue? = null,
) {
    override fun toString(): String = "PlayerSource(uri=<redacted>, headers=<redacted>)"
}
