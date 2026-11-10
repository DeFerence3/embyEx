package app.deference.embycl.ui.player

/** The subtitle style as the overlays use it: the current value and where a change goes. */
internal data class PlayerSubtitleStyleControls(
    val style: PlayerSubtitleStyle,
    val onStyleChange: (PlayerSubtitleStyle) -> Unit,
)
