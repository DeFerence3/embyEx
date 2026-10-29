package app.deference.embycl.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_seek_backward_amount
import embyex.player.generated.resources.player_seek_forward_amount
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
internal fun AndroidPlayerSeekFeedback(
    seconds: Int,
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    // Retain the last direction and label during the exit fade.
    val shown = remember(key1 = trigger) { seconds }
    val forward = shown > 0
    val description = stringResource(
        resource = if (forward) Res.string.player_seek_forward_amount else Res.string.player_seek_backward_amount,
        abs(shown),
    )
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = seconds != 0,
            modifier = Modifier.align(alignment = if (forward) Alignment.CenterEnd else Alignment.CenterStart),
            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = 0.35f)
                    .drawWithCache {
                        val edge = size.width * 0.9f
                        val oval = Path().apply {
                            moveTo(x = 0f, y = 0f)
                            lineTo(x = 0f, y = size.height)
                            lineTo(x = edge, y = size.height)
                            cubicTo(
                                x1 = edge, y1 = size.height,
                                x2 = size.width, y2 = size.height / 2f,
                                x3 = edge, y3 = 0f,
                            )
                            close()
                        }
                        onDrawBehind {
                            if (forward) {
                                drawContext.canvas.save()
                                drawContext.canvas.translate(dx = size.width, dy = 0f)
                                drawContext.canvas.scale(sx = -1f, sy = 1f)
                            }
                            drawPath(path = oval, color = PlayerColors.seekHighlight)
                            if (forward) drawContext.canvas.restore()
                        }
                    }
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!forward) SeekChevrons(forward = false, trigger = trigger)
                    SeekAmount(seconds = shown, trigger = trigger)
                    if (forward) SeekChevrons(forward = true, trigger = trigger)
                }
            }
        }
    }
}

@Composable
private fun SeekAmount(
    seconds: Int,
    trigger: Int,
) {
    val scale = remember { Animatable(initialValue = 1f) }
    LaunchedEffect(key1 = trigger) {
        scale.animateTo(targetValue = 1.2f, animationSpec = tween(durationMillis = 100))
        scale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 150))
    }
    Text(
        text = "${if (seconds > 0) "+" else "−"} ${abs(seconds)}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = PlayerColors.content,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

@Composable
private fun SeekChevrons(
    forward: Boolean,
    trigger: Int,
) {
    val progress = remember { Animatable(initialValue = 0f) }
    LaunchedEffect(key1 = trigger) {
        progress.snapTo(targetValue = 0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        )
    }
    val icon = if (forward) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft
    Box(modifier = Modifier.size(size = 48.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.content, modifier = Modifier.fillMaxSize())
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PlayerColors.content,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (if (forward) -15.dp.toPx() else 15.dp.toPx()) * (1f - progress.value)
                    alpha = 1f - progress.value
                },
        )
    }
}
