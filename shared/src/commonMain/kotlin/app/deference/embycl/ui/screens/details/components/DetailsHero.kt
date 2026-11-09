package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
internal fun DetailsHero(
    backdropUrl: String?,
    layout: DetailsLayout,
    viewportHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    when (layout) {
        DetailsLayout.Compact,
        DetailsLayout.Medium,
        -> {
            val artHeight = minOf(
                if (layout == DetailsLayout.Compact) 380.dp else 480.dp,
                viewportHeight * 0.55f,
            )
            val overlap = if (layout == DetailsLayout.Compact) 72.dp else 120.dp
            Box(modifier = modifier.fillMaxWidth()) {
                Backdrop(
                    url = backdropUrl,
                    scrim = Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to background,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = artHeight),
                )
                Column(
                    modifier = Modifier
                        .padding(
                            top = artHeight - overlap,
                            start = if (layout == DetailsLayout.Compact) 16.dp else 32.dp,
                            end = if (layout == DetailsLayout.Compact) 16.dp else 32.dp,
                        )
                        .widthIn(max = 640.dp),
                    content = content,
                )
            }
        }
        DetailsLayout.Expanded -> Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = viewportHeight * 0.88f),
        ) {
            Backdrop(
                url = backdropUrl,
                scrim = Brush.horizontalGradient(
                    0f to background,
                    0.45f to Color.Transparent,
                ),
                bottomScrim = Brush.verticalGradient(
                    0.6f to Color.Transparent,
                    1f to background,
                ),
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 280.dp),
            )
            Column(
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .statusBarsPadding()
                    .padding(
                        start = 48.dp,
                        end = 48.dp,
                        top = 72.dp,
                        bottom = 24.dp,
                    )
                    .widthIn(max = 600.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun Backdrop(
    url: String?,
    scrim: Brush,
    modifier: Modifier = Modifier,
    bottomScrim: Brush? = null,
) {
    Box(
        modifier = modifier.background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        ScrimLayer(brush = scrim)
        bottomScrim?.let { brush -> ScrimLayer(brush = brush) }
    }
}

@Composable
private fun BoxScope.ScrimLayer(brush: Brush) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .fillMaxHeight()
            .background(brush = brush),
    )
}
