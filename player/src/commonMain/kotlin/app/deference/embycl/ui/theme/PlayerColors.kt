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
    val transparent = Color.Transparent
}
