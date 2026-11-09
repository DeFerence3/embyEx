package app.deference.embycl.ui.player

internal data class MediaSessionSnapshot(
    val title: String,
    val status: MediaSessionStatus,
    val positionMillis: Long,
    val durationMillis: Long?,
    val speed: Float,
)
