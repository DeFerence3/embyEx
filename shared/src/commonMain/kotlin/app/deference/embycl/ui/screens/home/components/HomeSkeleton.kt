package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_loading
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeSkeleton(
    layout: HomeLayout,
    modifier: Modifier = Modifier,
) {
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    val label = stringResource(resource = Res.string.home_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = if (layout == HomeLayout.Phone) 260.dp else 320.dp)
                .background(color = block),
        )
        Column(
            modifier = Modifier.padding(horizontal = layout.gutter),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Bar(widthFraction = 0.5f, height = 28.dp)
            Bar(widthFraction = 0.8f, height = 40.dp)
            Bar(widthFraction = 1f, height = 64.dp)
        }
        Row(
            modifier = Modifier.padding(horizontal = layout.gutter),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            repeat(times = 4) {
                Box(
                    modifier = Modifier
                        .width(width = layout.posterWidth)
                        .aspectRatio(ratio = 2f / 3f)
                        .clip(shape = MaterialTheme.shapes.medium)
                        .background(color = block),
                )
            }
        }
    }
}

@Composable
private fun Bar(
    widthFraction: Float,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction = widthFraction)
            .height(height = height)
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}
