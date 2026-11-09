package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_load_failed_body
import embyex.shared.generated.resources.details_load_failed_title
import embyex.shared.generated.resources.details_loading
import embyex.shared.generated.resources.details_try_again
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsLoading(
    layout: DetailsLayout,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(resource = Res.string.details_loading)
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    val side = if (layout == DetailsLayout.Compact) 16.dp else 32.dp
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Surface(
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .height(height = if (layout == DetailsLayout.Compact) 300.dp else 420.dp),
        ) {}
        Column(
            modifier = Modifier
                .padding(horizontal = side)
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Block(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.7f)
                    .height(height = 40.dp),
            )
            Block(
                color = color,
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.5f)
                    .height(height = 16.dp),
            )
            Block(
                color = color,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 56.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                repeat(times = 3) {
                    Block(
                        color = color,
                        modifier = Modifier
                            .weight(weight = 1f)
                            .height(height = 64.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(height = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                repeat(times = 3) {
                    Block(
                        color = color,
                        modifier = Modifier
                            .width(width = 104.dp)
                            .aspectRatio(ratio = 3f / 4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Block(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    Surface(
        color = color,
        shape = shape,
        modifier = modifier,
    ) {}
}

@Composable
internal fun DetailsLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            modifier = Modifier
                .padding(top = 72.dp)
                .widthIn(max = 420.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(size = 48.dp),
            )
            Text(
                text = stringResource(resource = Res.string.details_load_failed_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(resource = Res.string.details_load_failed_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(resource = Res.string.details_try_again))
            }
        }
    }
}
