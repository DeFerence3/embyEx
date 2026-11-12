package app.deference.embycl.ui.core.components

import androidx.compose.foundation.shape.CircleShape
import app.deference.embycl.ui.core.focusRing
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.shelf_scroll_back
import embyex.shared.generated.resources.shelf_scroll_forward
import embyex.shared.generated.resources.shelf_see_all
import embyex.shared.generated.resources.shelf_see_all_label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShelfHeader(
    title: String,
    listState: LazyListState,
    showScrollButtons: Boolean,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
    count: String? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Row(
            modifier = Modifier.weight(weight = 1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(
                        weight = 1f,
                        fill = false,
                    )
                    .semantics { heading() },
            )
            if (count != null) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
        if (onSeeAll != null) {
            val label = stringResource(
                resource = Res.string.shelf_see_all_label,
                title,
            )
            TextButton(
                onClick = onSeeAll,
                modifier = Modifier
                    .focusRing(shape = CircleShape)
                    .semantics { contentDescription = label },
            ) {
                Text(text = stringResource(resource = Res.string.shelf_see_all))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            }
        }
        if (showScrollButtons && (listState.canScrollBackward || listState.canScrollForward)) {
            FilledTonalIconButton(
                onClick = { scope.launch { listState.scrollByPage(forward = false) } },
                enabled = listState.canScrollBackward,
                modifier = Modifier.focusRing(shape = CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(
                        resource = Res.string.shelf_scroll_back,
                        title,
                    ),
                )
            }
            FilledTonalIconButton(
                onClick = { scope.launch { listState.scrollByPage(forward = true) } },
                enabled = listState.canScrollForward,
                modifier = Modifier.focusRing(shape = CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(
                        resource = Res.string.shelf_scroll_forward,
                        title,
                    ),
                )
            }
        }
    }
}

private suspend fun LazyListState.scrollByPage(forward: Boolean) {
    val visible = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val distance = visible * 0.8f
    animateScrollBy(value = if (forward) distance else -distance)
}
