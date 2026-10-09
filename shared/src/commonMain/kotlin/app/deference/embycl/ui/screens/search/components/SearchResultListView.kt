package app.deference.embycl.ui.screens.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration

@OptIn(ExperimentalGridApi::class)
@Composable
fun SearchResultListView(
	type: String,
	header: String,
	results: List<EmbyItem>,
	onItemClick: (EmbyItem) -> Unit
) {
	val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
	val configuration = DeviceConfiguration.fromWindowSizeClass(windowSizeClass)
	
	/*LazyVerticalGrid(
		columns = GridCells.Adaptive(140.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)),
		modifier = Modifier.weight(1f),
		contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp + bottomSpacing),
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalArrangement = Arrangement.spacedBy(18.dp),
	) {
		groups.filterKeys { activeType == null || it == activeType }.forEach { (type, results) ->
			item(key = "heading:$type", span = { GridItemSpan(maxLineSpan) }) { SectionHeading(searchTypeLabel(type), detail = "${results.size}") }
			items(results, key = { "${type}:${it.id}" }, span = { GridItemSpan(if (type == "Episode") maxLineSpan else 1) }) { item ->
				MediaCard(item) { onItemClick(item) }
			}
		}
	}*/
	
	Column{
	}
}

