package app.deference.embycl.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import kotlin.math.abs
import kotlin.math.roundToInt

/** The bundled subtitle face: a static wght 700 instance of Nunito. libass matches it by this full name. */
private const val SUBTITLE_FONT_FULL_NAME = "Nunito Bold"
private const val SUBTITLE_FONT_FILE = "subtitle_nunito_bold.ttf"

/** mpv's `sub-margin-x` default, in scaled pixels. */
private const val DEFAULT_SIDE_MARGIN = 19

/**
 * The widest side margin, in scaled pixels. mpv applies `sub-margin-x` to both sides, so a wider margin also
 * narrows the space a line can use before it wraps; 192 keeps 70 % of a 16:9 picture.
 */
private const val MAX_SIDE_MARGIN = 192

/** Width of a 16:9 picture in mpv scaled pixels (720 px tall reference). */
internal const val SUBTITLE_REFERENCE_WIDTH = 1280

/**
 * mpv properties for [this] style; desktop and Android both run libmpv 0.41. They apply live to text
 * subtitles. Sizes and margins are mpv scaled pixels (720 px tall reference); colours are `#AARRGGBB`.
 *
 * mpv can't move a centred subtitle sideways, so an off-centre [PlayerSubtitleStyle.horizontalPercent]
 * anchors the subtitle to that side, [sideMargin] away from the edge.
 */
internal fun PlayerSubtitleStyle.mpvProperties(): List<Pair<String, String>> {
    val common = listOf(
        "sub-font" to SUBTITLE_FONT_FULL_NAME,
        // The bundled face is already bold; libass would otherwise embolden it again.
        "sub-bold" to "no",
        "sub-font-size" to "38",
        "sub-scale" to (sizePercent / 100.0).toString(),
        "sub-color" to PlayerColors.subtitleText.mpvColor(),
        // 100 is mpv's default bottom position; mpv keeps lower values on screen.
        "sub-pos" to (100 - verticalPercent).toString(),
        "sub-align-x" to when {
            horizontalPercent < 0 -> "left"
            horizontalPercent > 0 -> "right"
            else -> "center"
        },
        "sub-margin-x" to sideMargin().toString(),
        "sub-justify" to when (alignment) {
            PlayerSubtitleAlignment.Start -> "left"
            PlayerSubtitleAlignment.Center -> "center"
            PlayerSubtitleAlignment.End -> "right"
        },
    )
    val background = when (background) {
        PlayerSubtitleBackground.Box -> listOf(
            // libass draws one box per line in the outline colour; the outline size is the box padding.
            "sub-border-style" to "opaque-box",
            "sub-outline-color" to PlayerColors.subtitleBox.mpvColor(),
            "sub-outline-size" to "6",
            "sub-shadow-offset" to "0",
        )
        PlayerSubtitleBackground.Outline -> listOf(
            "sub-border-style" to "outline-and-shadow",
            "sub-outline-color" to PlayerColors.subtitleOutline.mpvColor(),
            "sub-outline-size" to "2.5",
            "sub-back-color" to PlayerColors.subtitleShadow.mpvColor(),
            "sub-shadow-offset" to "1",
        )
        PlayerSubtitleBackground.Shadow -> listOf(
            "sub-border-style" to "outline-and-shadow",
            "sub-outline-size" to "0",
            "sub-back-color" to PlayerColors.subtitleShadow.mpvColor(),
            "sub-shadow-offset" to "2.5",
        )
    }
    return common + background
}

/**
 * Distance in scaled pixels between the anchored side of the subtitle and the picture edge: the widest margin
 * just off centre, down to mpv's default at either end of the slider.
 */
internal fun PlayerSubtitleStyle.sideMargin(): Int {
    val distance = abs(horizontalPercent)
    if (distance == 0) return DEFAULT_SIDE_MARGIN
    return DEFAULT_SIDE_MARGIN + ((MAX_SIDE_MARGIN - DEFAULT_SIDE_MARGIN) * (100 - distance) / 100.0).roundToInt()
}

/** Directory name, under a player-owned cache directory, that mpv's `sub-fonts-dir` points at. */
internal const val SUBTITLE_FONTS_DIRECTORY = "subtitle-fonts"

/** The bundled subtitle fonts by file name, for the platform to write into [SUBTITLE_FONTS_DIRECTORY]. */
internal suspend fun readSubtitleFonts(): Map<String, ByteArray> =
    mapOf(SUBTITLE_FONT_FILE to Res.readBytes(path = "font/$SUBTITLE_FONT_FILE"))

private fun Color.mpvColor(): String {
    val argb = toArgb().toLong() and 0xFFFFFFFFL
    return "#" + argb.toString(radix = 16).uppercase().padStart(length = 8, padChar = '0')
}
