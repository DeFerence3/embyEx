package app.deference.embycl.ui.player

/** IDs are opaque and scoped to the currently opened source. */
internal data class PlayerTrack(
    val id: String,
    val name: String? = null,
    val language: String? = null,
    val labels: List<PlayerTrackLabel> = emptyList(),
)
