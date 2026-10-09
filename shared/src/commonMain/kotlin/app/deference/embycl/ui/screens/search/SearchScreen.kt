package app.deference.embycl.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.DESKTOP
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.MOBILE_PORTRAIT
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_LANDSCAPE
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration.TABLET_PORTRAIT
import app.deference.embycl.ui.core.components.EmptyState
import app.deference.embycl.ui.core.components.ErrorState
import app.deference.embycl.ui.core.components.ExpressiveLoading
import app.deference.embycl.ui.core.components.MediaCard
import app.deference.embycl.ui.core.components.SectionHeading

@Composable
fun SearchContent(
	state: SearchState,
	onAction: (SearchAction) -> Unit,
	modifier: Modifier = Modifier,
	bottomSpacing: Dp = 0.dp,
	onItemClick: (EmbyItem) -> Unit,
) {
	val focusRequester = remember { FocusRequester() }
	val focusManager = LocalFocusManager.current
	var selectedType by remember { mutableStateOf<String?>(null) }
	val groups = remember(state.results) { state.results.groupBy { it.type } }
	val activeType = selectedType?.takeIf { it in groups }
	LaunchedEffect(Unit) { focusRequester.requestFocus() }
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(horizontal = 20.dp)
			.imePadding(),
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		OutlinedTextField(
			value = state.query,
			onValueChange = { onAction(SearchAction.QueryChanged(it)) },
			modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
			shape = MaterialTheme.shapes.extraLarge,
			colors = OutlinedTextFieldDefaults.colors(
				focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
				unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
			),
			label = { Text("Search your library") },
			leadingIcon = { Icon(Icons.Default.Search, null) },
			trailingIcon = {
				if (state.query.isNotEmpty()) IconButton(onClick = {
					selectedType = null
					onAction(SearchAction.QueryChanged(""))
					focusRequester.requestFocus()
				}) { Icon(Icons.Default.Close, "Clear search") }
			},
			singleLine = true,
			keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
			keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); onAction(SearchAction.Retry) }),
		)
		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth()) {
			when {
				state.isLoading -> ExpressiveLoading(Modifier.align(Alignment.Center))
				state.error != null -> ErrorState(state.error) { onAction(SearchAction.Retry) }
				state.query.isBlank() -> EmptyState("What are you in the mood for?", "Search movies, shows, seasons, and episodes.")
				state.results.isEmpty() -> EmptyState("No matches yet", "Try a different title or a shorter search.")
				else -> Column {
					LazyRow(
//						contentPadding = PaddingValues(horizontal = 20.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						item {
							FilterChip(
								selected = activeType == null,
								onClick = { selectedType = null },
								label = { Text("All (${state.results.size})") }
							)
						}
						items(
							items = groups.keys.toList(),
//							key = { it }
						) { type ->
							FilterChip(selected = type == activeType, onClick = {
								selectedType = type
							}, label = { Text("${searchTypeLabel(type)} (${groups[type].orEmpty().size})") })
						}
					}
					LazyColumn(
						modifier = Modifier
							.fillMaxWidth(),
						verticalArrangement = Arrangement.spacedBy(16.dp)
					) {
						groups.filterKeys { activeType == null || it == activeType }.forEach { (type, results) ->
							stickyHeader(
//								key = "heading:${type}",
							) {
								SectionHeading(
									title = searchTypeLabel(type),
									detail = "${results.size}"
								)
							}
							item(
//								key = { "items:${type}" },
							) {
								SearchResultItem(type, results, onItemClick)
							}
						}
						item {
							Spacer(modifier = Modifier.height(bottomSpacing))
						}
					}
				}
			}
		}
	}
}


@OptIn(ExperimentalGridApi::class)
@Composable
fun SearchResultItem(
	type: String,
	results: List<EmbyItem>,
	onItemClick: (EmbyItem) -> Unit
) {
	val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
	val configuration = DeviceConfiguration.fromWindowSizeClass(windowSizeClass)
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

private fun searchTypeLabel(type: String) = when (type) {
	"Movie" -> "Movies"
	"Series" -> "Shows"
	"Season" -> "Seasons"
	"Episode" -> "Episodes"
	else -> type
}