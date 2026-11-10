package app.deference.embycl.ui.player

/**
 * How text subtitles (SRT, WebVTT and similar) are drawn. ASS/SSA tracks keep their authored styling, and
 * picture-based tracks (PGS, VobSub) can't be restyled.
 *
 * [sizePercent] scales the default size, within [SizePercents]. [verticalPercent] is the height above the
 * default bottom position as a share of the picture, within [VerticalPercents] (0 is the bottom, 100 the top).
 * [horizontalPercent] is within [HorizontalPercents]: 0 is centred, negative values move the subtitle towards
 * the left edge and positive values towards the right edge.
 */
data class PlayerSubtitleStyle(
    val sizePercent: Int = 100,
    val background: PlayerSubtitleBackground = PlayerSubtitleBackground.Box,
    val verticalPercent: Int = 0,
    val horizontalPercent: Int = 0,
    val alignment: PlayerSubtitleAlignment = PlayerSubtitleAlignment.Center,
) {
    /** This style with every number moved into its range. */
    fun coerced(): PlayerSubtitleStyle = copy(
        sizePercent = sizePercent.coerceIn(range = SizePercents),
        verticalPercent = verticalPercent.coerceIn(range = VerticalPercents),
        horizontalPercent = horizontalPercent.coerceIn(range = HorizontalPercents),
    )

    companion object {
        val Default = PlayerSubtitleStyle()
        val SizePercents = 50..200
        val VerticalPercents = 0..100
        val HorizontalPercents = -100..100

        /** The sliders move in steps of this many percent. */
        const val STEP_PERCENT = 5
    }
}
