package app.deference.embycl.ui.theme

import androidx.compose.ui.graphics.Color

/** Video chrome keeps the same contrast in both application themes. */
internal object PlayerColors {
    val background = Color.Black
    val content = Color.White
    val controlsSurface = Color.Black.copy(alpha = 0.55f)
    val controlsOutline = Color.White.copy(alpha = 0.2f)
    val seekHighlight = Color.White.copy(alpha = 0.2f)
    val controlsScrim = Color.Black.copy(alpha = 0.8f)
    val dialogScrim = Color.Black.copy(alpha = 0.6f)
    val transparent = Color.Transparent

    // Subtitles: also handed to mpv, so the preview and the video draw the same colours.
    val subtitleText = Color.White
    val subtitleBox = Color.Black.copy(alpha = 0.64f)
    val subtitleOutline = Color.Black
    val subtitleShadow = Color.Black.copy(alpha = 0.75f)
}
