package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_scroll_back
import embyex.shared.generated.resources.details_scroll_forward
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsRailHeader(
    title: String,
    listState: LazyListState,
    showScrollButtons: Boolean,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            text = title,
            modifier = Modifier.weight(weight = 1f),
        )
        if (showScrollButtons && (listState.canScrollBackward || listState.canScrollForward)) {
            IconButton(
                onClick = { scope.launch { listState.scrollByPage(forward = false) } },
                enabled = listState.canScrollBackward,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(resource = Res.string.details_scroll_back),
                )
            }
            IconButton(
                onClick = { scope.launch { listState.scrollByPage(forward = true) } },
                enabled = listState.canScrollForward,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(resource = Res.string.details_scroll_forward),
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
