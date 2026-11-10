package app.deference.embycl.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import app.deference.embycl.ui.player.PlayerSubtitleAlignment
import app.deference.embycl.ui.player.PlayerSubtitleBackground
import app.deference.embycl.ui.player.PlayerSubtitleStyle
import app.deference.embycl.ui.player.SUBTITLE_REFERENCE_WIDTH
import app.deference.embycl.ui.player.sideMargin
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_subtitle_alignment
import embyex.player.generated.resources.player_subtitle_alignment_center
import embyex.player.generated.resources.player_subtitle_alignment_end
import embyex.player.generated.resources.player_subtitle_alignment_start
import embyex.player.generated.resources.player_subtitle_background
import embyex.player.generated.resources.player_subtitle_background_box
import embyex.player.generated.resources.player_subtitle_background_outline
import embyex.player.generated.resources.player_subtitle_background_shadow
import embyex.player.generated.resources.player_subtitle_horizontal
import embyex.player.generated.resources.player_subtitle_horizontal_center
import embyex.player.generated.resources.player_subtitle_horizontal_left
import embyex.player.generated.resources.player_subtitle_horizontal_right
import embyex.player.generated.resources.player_subtitle_percent
import embyex.player.generated.resources.player_subtitle_preview
import embyex.player.generated.resources.player_subtitle_reset
import embyex.player.generated.resources.player_subtitle_size
import embyex.player.generated.resources.player_subtitle_style_note
import embyex.player.generated.resources.player_subtitle_undo_reset
import embyex.player.generated.resources.player_subtitle_vertical
import embyex.player.generated.resources.player_subtitle_vertical_bottom
import embyex.player.generated.resources.player_subtitle_vertical_top
import embyex.player.generated.resources.subtitle_nunito_bold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Subtitle style page: live preview, size, position, alignment, background and reset. Used inside the
 * desktop Subtitles / More menu and the Android sheet.
 *
 * Choices apply at once. A slider moves the preview while it is dragged and reaches [onStyleChange] when it is
 * let go, so the style is saved once per drag.
 *
 * The page paints its own panel in [containerColor] and [containerShape], leaving the preview see-through so
 * the line sits over the playing video; the host's menu or sheet container must be transparent. [header]
 * (title, back row, drag handle) is drawn on that panel, and [modifier] applies inside it.
 */
@Composable
internal fun PlayerSubtitleStylePage(
    style: PlayerSubtitleStyle,
    onStyleChange: (PlayerSubtitleStyle) -> Unit,
    containerColor: Color,
    containerShape: Shape,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    val previewShape = MaterialTheme.shapes.medium
    val layout = remember { SeeThroughLayout() }
    // The style before the last reset, offered as Undo until something else changes.
    var beforeReset by remember { mutableStateOf<PlayerSubtitleStyle?>(value = null) }
    // What the sliders show while one is dragged; replaced whenever the saved style changes.
    var draft by remember(key1 = style) { mutableStateOf(value = style) }
    val change: (PlayerSubtitleStyle) -> Unit = { next ->
        beforeReset = null
        onStyleChange(next)
    }
    val commitDraft = { if (draft != style) change(draft) }
    CompositionLocalProvider(LocalContentColor provides contentColorFor(backgroundColor = containerColor)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layout.page = it }
                .panelWithHole(
                    color = containerColor,
                    shape = containerShape,
                    hole = { layout.preview },
                    holeShape = previewShape,
                )
                .then(other = modifier),
        ) {
            header()
            SubtitlePreview(
                style = draft,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { layout.place(preview = it) },
            )
            StyleSlider(
                label = stringResource(resource = Res.string.player_subtitle_size),
                readout = stringResource(Res.string.player_subtitle_percent, draft.sizePercent),
                value = draft.sizePercent,
                range = PlayerSubtitleStyle.SizePercents,
                onValueChange = { draft = draft.copy(sizePercent = it) },
                onValueChangeFinished = commitDraft,
            )
            StyleSlider(
                label = stringResource(resource = Res.string.player_subtitle_vertical),
                readout = verticalReadout(percent = draft.verticalPercent),
                value = draft.verticalPercent,
                range = PlayerSubtitleStyle.VerticalPercents,
                onValueChange = { draft = draft.copy(verticalPercent = it) },
                onValueChangeFinished = commitDraft,
            )
            StyleSlider(
                label = stringResource(resource = Res.string.player_subtitle_horizontal),
                readout = horizontalReadout(percent = draft.horizontalPercent),
                value = draft.horizontalPercent,
                range = PlayerSubtitleStyle.HorizontalPercents,
                onValueChange = { draft = draft.copy(horizontalPercent = it) },
                onValueChangeFinished = commitDraft,
            )
            SectionLabel(text = stringResource(resource = Res.string.player_subtitle_alignment))
            StyleSegments(
                options = PlayerSubtitleAlignment.entries,
                selected = style.alignment,
                onSelect = { change(style.copy(alignment = it)) },
                showSelectedIcon = false,
            ) { alignment ->
                Icon(
                    imageVector = alignment.icon,
                    contentDescription = stringResource(resource = alignment.label()),
                )
            }
            SectionLabel(text = stringResource(resource = Res.string.player_subtitle_background))
            StyleSegments(
                options = PlayerSubtitleBackground.entries,
                selected = style.background,
                onSelect = { change(style.copy(background = it)) },
            ) { background ->
                Text(
                    text = stringResource(resource = background.label()),
                    maxLines = 1,
                )
            }
            Text(
                text = stringResource(resource = Res.string.player_subtitle_style_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            val undo = beforeReset
            TextButton(
                onClick = {
                    if (undo != null) {
                        beforeReset = null
                        onStyleChange(undo)
                    } else {
                        beforeReset = style
                        onStyleChange(PlayerSubtitleStyle.Default)
                    }
                },
                enabled = undo != null || style != PlayerSubtitleStyle.Default,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = if (undo != null) Icons.AutoMirrored.Filled.Undo else Icons.Default.RestartAlt,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(resource = if (undo != null) Res.string.player_subtitle_undo_reset else Res.string.player_subtitle_reset),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** Tracks where the preview sits inside the page, so the panel can leave that area unpainted. */
private class SeeThroughLayout {
    var page: LayoutCoordinates? = null
        set(value) {
            field = value
            previewCoordinates?.let { place(preview = it) }
        }
    private var previewCoordinates: LayoutCoordinates? = null
    var preview by mutableStateOf<Rect?>(value = null)
        private set

    fun place(
        preview: LayoutCoordinates,
    ) {
        previewCoordinates = preview
        val parent = page?.takeIf { it.isAttached && preview.isAttached } ?: return
        this.preview = parent.localBoundingBoxOf(sourceCoordinates = preview, clipBounds = false)
    }
}

/** Fills [shape] with [color] except for [hole], which stays transparent. */
private fun Modifier.panelWithHole(
    color: Color,
    shape: Shape,
    hole: () -> Rect?,
    holeShape: Shape,
): Modifier = drawWithCache {
    val panel = Path().apply { addOutline(outline = shape.createOutline(size = size, layoutDirection = layoutDirection, density = this@drawWithCache)) }
    val area = hole()
    val path = if (area == null) panel else {
        val cut = Path().apply {
            addOutline(outline = holeShape.createOutline(size = area.size, layoutDirection = layoutDirection, density = this@drawWithCache))
            translate(offset = area.topLeft)
        }
        Path.combine(operation = PathOperation.Difference, path1 = panel, path2 = cut)
    }
    onDrawBehind { drawPath(path = path, color = color) }
}

/**
 * The sample line placed in a small picture the way mpv places it on the video. Decorative: the controls
 * below say what is chosen.
 */
@Composable
private fun SubtitlePreview(
    style: PlayerSubtitleStyle,
    modifier: Modifier = Modifier,
) {
    // No fill: the page leaves this area see-through, so the line sits over the playing video.
    // A plain Box, not BoxWithConstraints: the desktop menu measures intrinsic sizes, which subcomposition can't.
    Box(
        modifier = modifier
            .height(height = 136.dp)
            .padding(all = 8.dp)
            .clearAndSetSemantics { },
    ) {
        val side = style.sideMargin().toFloat() / SUBTITLE_REFERENCE_WIDTH
        val horizontalBias = when {
            style.horizontalPercent < 0 -> -1f
            style.horizontalPercent > 0 -> 1f
            else -> 0f
        }
        PlayerSubtitleText(
            text = stringResource(resource = Res.string.player_subtitle_preview),
            style = style,
            modifier = Modifier
                .align(
                    alignment = BiasAbsoluteAlignment(
                        horizontalBias = horizontalBias,
                        verticalBias = 1f - 2f * style.verticalPercent / 100f,
                    ),
                )
                .absoluteInset(
                    left = if (horizontalBias <= 0f) side else 0f,
                    right = if (horizontalBias >= 0f) side else 0f,
                ),
        )
    }
}

/**
 * Keeps [left] and [right] fractions of the available width free on those screen sides, whatever the layout
 * direction, as mpv does. Without a bounded width (an intrinsic measurement) there is nothing to keep.
 */
private fun Modifier.absoluteInset(
    left: Float,
    right: Float,
): Modifier = layout { measurable, constraints ->
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    val start = (width * left).roundToInt()
    val end = (width * right).roundToInt()
    val placeable = measurable.measure(constraints = constraints.offset(horizontal = -(start + end)))
    layout(
        width = constraints.constrainWidth(width = placeable.width + start + end),
        height = placeable.height,
    ) {
        placeable.place(x = start, y = 0)
    }
}

@Composable
private fun PlayerSubtitleText(
    text: String,
    style: PlayerSubtitleStyle,
    modifier: Modifier = Modifier,
) {
    val base = TextStyle(
        fontFamily = FontFamily(
            Font(
                resource = Res.font.subtitle_nunito_bold,
                weight = FontWeight.Bold,
            ),
        ),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp * (style.sizePercent / 100f),
        textAlign = when (style.alignment) {
            PlayerSubtitleAlignment.Start -> TextAlign.Left
            PlayerSubtitleAlignment.Center -> TextAlign.Center
            PlayerSubtitleAlignment.End -> TextAlign.Right
        },
        color = PlayerColors.subtitleText,
    )
    when (style.background) {
        PlayerSubtitleBackground.Box -> Text(
            text = text,
            style = base,
            modifier = modifier
                .background(
                    color = PlayerColors.subtitleBox,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        PlayerSubtitleBackground.Outline -> Box(modifier = modifier) {
            Text(
                text = text,
                style = base.copy(
                    color = PlayerColors.subtitleOutline,
                    drawStyle = Stroke(width = 5f),
                ),
            )
            Text(
                text = text,
                style = base,
            )
        }
        PlayerSubtitleBackground.Shadow -> Text(
            text = text,
            style = base.copy(
                shadow = Shadow(
                    color = PlayerColors.subtitleShadow,
                    offset = Offset(x = 0f, y = 3f),
                    blurRadius = 6f,
                ),
            ),
            modifier = modifier,
        )
    }
}

/** A labelled slider over [range] that snaps to [PlayerSubtitleStyle.STEP_PERCENT] steps, with [readout] beside the label. */
@Composable
private fun StyleSlider(
    label: String,
    readout: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(weight = 1f),
        )
        Text(
            text = readout,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    Slider(
        value = value.toFloat(),
        // Snapped here rather than with `steps`, which would draw a tick for every step.
        onValueChange = { onValueChange((it / PlayerSubtitleStyle.STEP_PERCENT).roundToInt() * PlayerSubtitleStyle.STEP_PERCENT) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics {
                contentDescription = label
                stateDescription = readout
            },
    )
}

@Composable
private fun <T> StyleSegments(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    showSelectedIcon: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                icon = { if (showSelectedIcon) SegmentedButtonDefaults.Icon(active = option == selected) },
                label = { content(option) },
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun verticalReadout(
    percent: Int,
): String = when (percent) {
    PlayerSubtitleStyle.VerticalPercents.first -> stringResource(resource = Res.string.player_subtitle_vertical_bottom)
    PlayerSubtitleStyle.VerticalPercents.last -> stringResource(resource = Res.string.player_subtitle_vertical_top)
    else -> stringResource(Res.string.player_subtitle_percent, percent)
}

@Composable
private fun horizontalReadout(
    percent: Int,
): String = when {
    percent < 0 -> stringResource(Res.string.player_subtitle_horizontal_left, abs(percent))
    percent > 0 -> stringResource(Res.string.player_subtitle_horizontal_right, percent)
    else -> stringResource(resource = Res.string.player_subtitle_horizontal_center)
}

private val PlayerSubtitleAlignment.icon: ImageVector
    get() = when (this) {
        PlayerSubtitleAlignment.Start -> Icons.AutoMirrored.Filled.FormatAlignLeft
        PlayerSubtitleAlignment.Center -> Icons.Default.FormatAlignCenter
        PlayerSubtitleAlignment.End -> Icons.AutoMirrored.Filled.FormatAlignRight
    }

private fun PlayerSubtitleAlignment.label(): StringResource = when (this) {
    PlayerSubtitleAlignment.Start -> Res.string.player_subtitle_alignment_start
    PlayerSubtitleAlignment.Center -> Res.string.player_subtitle_alignment_center
    PlayerSubtitleAlignment.End -> Res.string.player_subtitle_alignment_end
}

private fun PlayerSubtitleBackground.label(): StringResource = when (this) {
    PlayerSubtitleBackground.Box -> Res.string.player_subtitle_background_box
    PlayerSubtitleBackground.Outline -> Res.string.player_subtitle_background_outline
    PlayerSubtitleBackground.Shadow -> Res.string.player_subtitle_background_shadow
}
