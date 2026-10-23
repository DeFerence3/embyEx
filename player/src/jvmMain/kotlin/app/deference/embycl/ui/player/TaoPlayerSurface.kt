package app.deference.embycl.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

@Composable
internal fun TaoPlayerSurface(
    player: TaoMpvPlayer,
    modifier: Modifier = Modifier,
) {
    val surface = remember(key1 = player) { player.surface() }
    val frame = player.frame.collectAsState()
    DisposableEffect(key1 = surface) {
        onDispose { surface.close() }
    }
    Canvas(
        modifier = modifier
            .onSizeChanged { size -> surface.resize(size.width, size.height) },
    ) {
        frame.value // Only invalidate drawing when the native producer publishes a frame.
        surface.draw { image ->
            val scale = minOf(size.width / image.width, size.height / image.height)
            val width = image.width * scale
            val height = image.height * scale
            drawIntoCanvas { canvas ->
                canvas.skiaCanvas.drawImageRect(
                    image = image,
                    src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    dst = Rect.makeXYWH((size.width - width) / 2f, (size.height - height) / 2f, width, height),
                    samplingMode = SamplingMode.LINEAR,
                    paint = null,
                    strict = true,
                )
            }
        }
    }
}
