package app.deference.embycl.ui.player

/** External subtitle description. URI values can contain credentials and must not be logged. */
data class PlayerSubtitle(
    val uri: String,
    val mimeType: String? = null,
    val language: String? = null,
    val label: String? = null,
) {
    override fun toString(): String = "PlayerSubtitle(uri=<redacted>)"
}
