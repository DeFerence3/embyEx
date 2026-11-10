package app.deference.embycl.ui.player

/** An episode before or after the playing one, as the Up next list shows it. */
data class PlayerQueueEntry(
    val id: String,
    /** Short position label, for example "S2 · E6". */
    val label: String,
    val title: String,
    val durationMs: Long? = null,
    /** Loaded through [LocalPlayerArtwork]; null shows a plain tile. */
    val artworkUri: String? = null,
)
