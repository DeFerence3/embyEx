package app.deference.embycl.ui.screens.home.seeall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalDeviceConfiguration
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.core.components.EmptyState
import app.deference.embycl.ui.core.components.ErrorState
import app.deference.embycl.ui.core.components.ExpressiveLoading
import app.deference.embycl.ui.core.components.MediaCard
import app.deference.embycl.ui.screens.openItem
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_see_all_empty_body
import embyex.shared.generated.resources.home_see_all_empty_title
import embyex.shared.generated.resources.home_see_all_failed
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class HomeSectionScreen(
    val key: String,
    val title: String,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<HomeSectionViewModel>(parameters = { parametersOf(key) })
        val state by viewModel.state.collectAsStateWithLifecycle()
        HomeSectionContent(
            title = title,
            state = state,
            onAction = viewModel::onAction,
            onBack = { navigator.goBack() },
            onOpen = { openItem(item = it, backStack = navigator) },
        )
    }
}

@Composable
internal fun HomeSectionContent(
    title: String,
    state: HomeSectionState,
    onAction: (HomeSectionAction) -> Unit,
    onBack: () -> Unit,
    onOpen: (EmbyItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = title,
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
        ) {
            val items = state.items
            when {
                items == null -> ExpressiveLoading(modifier = Modifier.align(alignment = Alignment.Center))
                items.isEmpty() && state.failed -> ErrorState(
                    message = stringResource(resource = Res.string.home_see_all_failed),
                    onRetry = { onAction(HomeSectionAction.Retry) },
                )
                items.isEmpty() -> EmptyState(
                    title = stringResource(resource = Res.string.home_see_all_empty_title),
                    message = stringResource(resource = Res.string.home_see_all_empty_body),
                )
                else -> SectionGrid(
                    items = items,
                    loadingMore = state.loadingMore,
                    canLoadMore = state.canLoadMore,
                    onLoadMore = { onAction(HomeSectionAction.LoadMore) },
                    onOpen = onOpen,
                )
            }
        }
    }
}

@Composable
private fun SectionGrid(
    items: List<EmbyItem>,
    loadingMore: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onOpen: (EmbyItem) -> Unit,
) {
    val episodes = items.isNotEmpty() && items.all { it.isEpisode() }
    // Same column rules as Library: episode lists by window size, posters adaptive to text size.
    val columns = if (episodes) {
        GridCells.Fixed(
            count = when (LocalDeviceConfiguration.current) {
                DeviceConfiguration.MOBILE_PORTRAIT,
                DeviceConfiguration.TABLET_PORTRAIT,
                -> 1
                DeviceConfiguration.MOBILE_LANDSCAPE -> 2
                DeviceConfiguration.TABLET_LANDSCAPE,
                DeviceConfiguration.DESKTOP,
                -> 3
            },
        )
    } else {
        GridCells.Adaptive(minSize = 144.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f))
    }
    val gridState = rememberLazyGridState()
    val nearEnd by remember(items.size) {
        derivedStateOf { (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= items.size - 6 }
    }
    LaunchedEffect(nearEnd, canLoadMore) {
        if (nearEnd && canLoadMore) onLoadMore()
    }
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 18.dp),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            MediaCard(item = item) { onOpen(item) }
        }
        if (loadingMore) {
            item(span = { GridItemSpan(currentLineSpan = maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}
