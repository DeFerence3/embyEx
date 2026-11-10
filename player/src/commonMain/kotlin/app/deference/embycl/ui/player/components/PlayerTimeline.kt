package app.deference.embycl.ui.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerChapter
import app.deference.embycl.ui.player.PlayerMediaStatus
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_position
import embyex.player.generated.resources.player_position_chapter
import embyex.player.generated.resources.player_position_value
import embyex.player.generated.resources.player_time_left
import embyex.player.generated.resources.player_time_total
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

/**
 * Full-width timeline with only the two timers beside it. The played part is wavy while [wavy] (playing),
 * flat otherwise. Chapter ticks mark the chapters still ahead. Keyboard and screen readers use the slider.
 */
@Composable
internal fun PlayerTimeline(
    player: EmbeddedPlayer,
    enabled: Boolean,
    onSeeking: (Boolean) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    chapters: List<PlayerChapter> = emptyList(),
    wavy: Boolean = false,
) {
    val position by player.currentPositionMillis.collectAsState()
    val properties by player.mediaProperties.collectAsState()
    val state by player.state.collectAsState()
    val duration = properties?.durationMillis ?: 0L
    var seeking by remember(key1 = player) { mutableStateOf<Float?>(value = null) }
    var remaining by remember { mutableStateOf(value = true) }
    val fraction = seeking ?: if (duration > 0L) (position.toDouble() / duration).toFloat().coerceIn(0f, 1f) else 0f
    val displayed = seeking?.let { (it * duration).toLong() } ?: position
    val chapterIndex = chapters.indexOfLast { it.startMillis <= displayed }
    val chapter = chapters.getOrNull(index = chapterIndex)
    val chapterName = chapter?.let { chapterTitle(name = it.name, number = chapterIndex + 1) }
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val interaction = remember { MutableInteractionSource() }
    val positionText = playerTime(milliseconds = displayed)
    val durationText = playerTime(milliseconds = duration)
    val label = stringResource(resource = Res.string.player_position)
    val valueText = if (chapter != null) {
        stringResource(Res.string.player_position_chapter, positionText, durationText, chapterIndex + 1, chapterName.orEmpty())
    } else {
        stringResource(Res.string.player_position_value, positionText, durationText)
    }
    val endText = if (remaining) "−" + playerTime(milliseconds = duration - displayed) else durationText
    val endDescription = if (remaining) {
        stringResource(Res.string.player_time_left, playerTime(milliseconds = duration - displayed))
    } else {
        stringResource(Res.string.player_time_total, durationText)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = positionText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 52.dp),
        )
        Box(modifier = Modifier.weight(weight = 1f)) {
            Slider(
                value = fraction,
                onValueChange = {
                    seeking = it
                    onSeeking(true)
                },
                onValueChangeFinished = {
                    seeking?.let { target ->
                        dispatchAction { player.seekTo(positionMillis = (target.toDouble() * duration).toLong()) }
                    }
                    seeking = null
                    onSeeking(false)
                },
                enabled = enabled && duration > 0 && state.mediaStatus == PlayerMediaStatus.Ready,
                interactionSource = interaction,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interaction,
                        colors = SliderDefaults.colors(thumbColor = active),
                        enabled = enabled,
                        thumbSize = DpSize(width = 6.dp, height = 28.dp),
                    )
                },
                track = {
                    TimelineTrack(
                        fraction = fraction,
                        wavy = wavy && seeking == null,
                        chapters = chapters,
                        durationMillis = duration,
                        active = active,
                        inactive = inactive,
                        tick = tick,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = label
                        stateDescription = valueText
                    },
            )
            if (seeking != null) SeekBubble(
                fraction = fraction,
                time = positionText,
                chapter = chapterName,
            )
        }
        Text(
            text = endText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClickLabel = endDescription) { remaining = !remaining }
                .semantics { contentDescription = endDescription }
                .height(height = 48.dp)
                .widthIn(min = 56.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
        )
    }
}

@Composable
private fun TimelineTrack(
    fraction: Float,
    wavy: Boolean,
    chapters: List<PlayerChapter>,
    durationMillis: Long,
    active: Color,
    inactive: Color,
    tick: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 28.dp),
    ) {
        val centre = size.height / 2
        val playhead = size.width * fraction
        val gap = 6.dp.toPx()
        val line = 6.dp.toPx()
        if (playhead + gap < size.width) drawLine(
            color = inactive,
            start = Offset(x = playhead + gap, y = centre),
            end = Offset(x = size.width, y = centre),
            strokeWidth = line,
            cap = StrokeCap.Round,
        )
        val playedEnd = (playhead - gap).coerceAtLeast(minimumValue = 0f)
        if (playedEnd > 0f) {
            if (wavy) {
                val amplitude = 3.dp.toPx()
                val wavelength = 24.dp.toPx()
                val path = Path().apply {
                    moveTo(x = 0f, y = centre)
                    var x = 0f
                    while (x <= playedEnd) {
                        lineTo(x = x, y = centre + amplitude * sin(x / wavelength * 2 * PI).toFloat())
                        x += 2f
                    }
                }
                drawPath(path = path, color = active, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
            } else {
                drawLine(
                    color = active,
                    start = Offset(x = 0f, y = centre),
                    end = Offset(x = playedEnd, y = centre),
                    strokeWidth = line,
                    cap = StrokeCap.Round,
                )
            }
        }
        if (durationMillis > 0L) chapters.drop(n = 1).forEach { chapter ->
            val x = size.width * (chapter.startMillis.toFloat() / durationMillis)
            if (x > playhead + gap) drawLine(
                color = tick,
                start = Offset(x = x, y = centre - 6.dp.toPx()),
                end = Offset(x = x, y = centre + 6.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Target time and chapter above the playhead while dragging or seeking by keyboard. */
@Composable
private fun SeekBubble(
    fraction: Float,
    time: String,
    chapter: String?,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 3.dp,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints = constraints.copy(minWidth = 0, minHeight = 0))
            val x = (constraints.maxWidth * fraction - placeable.width / 2f).toInt()
                .coerceIn(minimumValue = 0, maximumValue = (constraints.maxWidth - placeable.width).coerceAtLeast(minimumValue = 0))
            layout(width = 0, height = 0) {
                placeable.place(x = x, y = -placeable.height)
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = time, style = MaterialTheme.typography.labelLarge)
            if (chapter != null) Text(text = chapter, style = MaterialTheme.typography.labelSmall)
        }
    }
}

internal fun playerTime(
    milliseconds: Long,
): String {
    val seconds = milliseconds.coerceAtLeast(minimumValue = 0L) / 1000L
    val minutes = (seconds / 60 % 60).toString().padStart(length = 2, padChar = '0')
    val remainder = (seconds % 60).toString().padStart(length = 2, padChar = '0')
    return if (seconds >= 3600) "${seconds / 3600}:$minutes:$remainder" else "$minutes:$remainder"
}
