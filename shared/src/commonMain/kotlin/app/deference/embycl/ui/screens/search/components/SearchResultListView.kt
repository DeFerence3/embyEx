package app.deference.embycl.ui.screens.search.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.DESKTOP
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_PORTRAIT
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_PORTRAIT
import app.deference.embycl.ui.core.components.MediaCard

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
	
	Column{
		Text(
			text = header,
			style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
		)
		when(type){
			"Episode" -> {
			
				val columns = when(configuration){
					MOBILE_PORTRAIT -> 1
					MOBILE_LANDSCAPE -> 2
					TABLET_PORTRAIT -> 1
					TABLET_LANDSCAPE -> 3
					DESKTOP -> 3
				}
				
				Grid(
					modifier = Modifier.fillMaxWidth(),
					config = {
						repeat(columns) {
							column((1).fr)
						}
						gap(12.dp)
					}
				) {
					results.forEach{ item ->
						MediaCard(item,modifier = Modifier.fillMaxWidth()) { onItemClick(item) }
					}
				}
			}
			
			else -> Grid(
				modifier = Modifier.fillMaxWidth(),
				config = {
					val minItemWidth = 128.dp
					val columnGap = 12.dp
					val rowGap = 16.dp
					
					val availableWidth = constraints.maxWidth.toDp()
					
					val columns = (
							(availableWidth + columnGap) /
									(minItemWidth + columnGap)
							).toInt().coerceAtLeast(1)
					
					val itemWidth =
						(availableWidth - columnGap * (columns - 1)) / columns
					
					repeat(columns) {
						column(itemWidth)
					}
					
					val rows = (results.size + columns - 1) / columns
					
					repeat(rows) {
						row(GridTrackSize.MaxContent)
					}
					
					gap(
						row = rowGap,
						column = columnGap
					)
				}
			) {
				results.forEach { item ->
					MediaCard(item) {
						onItemClick(item)
					}
				}
			}
		}
	}
}