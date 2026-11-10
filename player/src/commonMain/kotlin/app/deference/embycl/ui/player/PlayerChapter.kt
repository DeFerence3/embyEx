package app.deference.embycl.ui.player

/** A chapter of the open media. [startMillis] is the offset from the start of the media. */
internal data class PlayerChapter(
    val name: String,
    val startMillis: Long,
)
