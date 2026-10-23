package app.deference.embycl.ui.player

internal data class AndroidPlayerFeedback(
    val message: String,
    val level: Float? = null,
    val onLeft: Boolean = false,
)
