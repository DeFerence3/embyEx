package app.deference.embycl.ui.screens.shell.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.launch

@Composable
internal fun EmbyTab(
	modifier: Modifier,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
	contentColor: Color = TabRowDefaults.primaryContentColor,
	shape: Shape = RectangleShape,
	indicator: @Composable TabIndicatorScope.() -> Unit = {},
	tabs: @Composable () -> Unit,
) {
	Surface(
		modifier = modifier.selectableGroup(),
		color = containerColor,
		contentColor = contentColor,
		shape = shape
	) {
		val tabIndicatorAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
		val scope = remember {
			object : TabIndicatorScope, TabPositionsHolder {
				val tabPositions = mutableStateOf<(List<TabPosition>)>(listOf())
				
				override fun Modifier.tabIndicatorLayout(
					measure:
					MeasureScope.(Measurable, Constraints, List<TabPosition>) -> MeasureResult
				): Modifier =
					this.layout { measurable: Measurable, constraints: Constraints ->
						measure(measurable, constraints, tabPositions.value)
					}
				
				override fun Modifier.tabIndicatorOffset(
					selectedTabIndex: Int,
					matchContentSize: Boolean,
				): Modifier =
					this.then(
						TabIndicatorModifier(
							tabPositions,
							selectedTabIndex,
							matchContentSize,
							tabIndicatorAnimationSpec,
						)
					)
				
				override fun setTabPositions(positions: List<TabPosition>) {
					tabPositions.value = positions
				}
			}
		}
		
		Layout(
			modifier = Modifier,
			contents = listOf(tabs, { scope.indicator() }),
		) { (tabMeasurables, indicatorMeasurables), constraints ->
			
			val tabConstraints = constraints.copy(
				minWidth = 0,
				minHeight = 0,
			)
			
			val tabPlaceables = tabMeasurables.fastMap {
				it.measure(tabConstraints)
			}
			
			val tabRowWidth = tabPlaceables.sumOf {
				it.width
			}
			
			val tabRowHeight = tabPlaceables.maxOfOrNull {
				it.height
			} ?: 0
			
			var currentX = 0
			
			val positions = tabPlaceables.fastMap { placeable ->
				
				val tabWidthPx = placeable.width
				
				val position = TabPosition(
					left = currentX.toDp(),
					width = tabWidthPx.toDp(),
					contentWidth = tabWidthPx.toDp(),
				)
				
				currentX += tabWidthPx
				
				position
			}
			
			scope.setTabPositions(positions)
			
			layout(tabRowWidth, tabRowHeight) {
				indicatorMeasurables.fastForEach {
					it.measure(
						Constraints(
							maxWidth = tabRowWidth,
							minHeight = tabRowHeight,
							maxHeight = tabRowHeight,
						)
					).placeRelative(0, 0)
				}
				var x = 0
				tabPlaceables.fastForEach { placeable ->
					val y = (tabRowHeight - placeable.height) / 2
					placeable.placeRelative(
						x,
						y
					)
					x += placeable.width
				}
			}
		}
	}
}

@Immutable
class TabPosition internal constructor(val left: Dp, val width: Dp, val contentWidth: Dp) {
	
	val right: Dp
		get() = left + width
	
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is TabPosition) return false
		
		if (left != other.left) return false
		if (width != other.width) return false
		if (contentWidth != other.contentWidth) return false
		
		return true
	}
	
	override fun hashCode(): Int {
		var result = left.hashCode()
		result = 31 * result + width.hashCode()
		result = 31 * result + contentWidth.hashCode()
		return result
	}
	
	override fun toString(): String {
		return "TabPosition(left=$left, right=$right, width=$width, contentWidth=$contentWidth)"
	}
}

interface TabIndicatorScope {
	
	fun Modifier.tabIndicatorLayout(
		measure: MeasureScope.(Measurable, Constraints, List<TabPosition>) -> MeasureResult
	): Modifier
	
	fun Modifier.tabIndicatorOffset(
		selectedTabIndex: Int,
		matchContentSize: Boolean = false,
	): Modifier
}

internal data class TabIndicatorModifier(
	val tabPositionsState: State<List<TabPosition>>,
	val selectedTabIndex: Int,
	val followContentSize: Boolean,
	val animationSpec: FiniteAnimationSpec<Dp>,
) : ModifierNodeElement<TabIndicatorOffsetNode>() {
	
	override fun create(): TabIndicatorOffsetNode {
		return TabIndicatorOffsetNode(
			tabPositionsState = tabPositionsState,
			selectedTabIndex = selectedTabIndex,
			followContentSize = followContentSize,
			animationSpec = animationSpec,
		)
	}
	
	override fun update(node: TabIndicatorOffsetNode) {
		node.tabPositionsState = tabPositionsState
		node.selectedTabIndex = selectedTabIndex
		node.followContentSize = followContentSize
		node.animationSpec = animationSpec
	}
	
	override fun InspectorInfo.inspectableProperties() {
		// Show nothing in the inspector.
	}
}

internal class TabIndicatorOffsetNode(
	var tabPositionsState: State<List<TabPosition>>,
	var selectedTabIndex: Int,
	var followContentSize: Boolean,
	var animationSpec: FiniteAnimationSpec<Dp>,
) : Modifier.Node(), LayoutModifierNode {
	
	private var offsetAnimatable: Animatable<Dp, AnimationVector1D>? = null
	private var widthAnimatable: Animatable<Dp, AnimationVector1D>? = null
	private var initialOffset: Dp? = null
	private var initialWidth: Dp? = null
	
	override fun MeasureScope.measure(
		measurable: Measurable,
		constraints: Constraints,
	): MeasureResult {
		if (tabPositionsState.value.isEmpty()) {
			return layout(0, 0) {}
		}
		
		val currentTabWidth =
			if (followContentSize) {
				tabPositionsState.value[selectedTabIndex].contentWidth
			} else {
				tabPositionsState.value[selectedTabIndex].width
			}
		
		if (initialWidth != null) {
			val widthAnim =
				widthAnimatable
					?: Animatable(initialWidth!!, Dp.VectorConverter).also { widthAnimatable = it }
			
			if (currentTabWidth != widthAnim.targetValue) {
				coroutineScope.launch { widthAnim.animateTo(currentTabWidth, animationSpec) }
			}
		} else {
			initialWidth = currentTabWidth
		}
		
		val indicatorOffset = tabPositionsState.value[selectedTabIndex].left
		
		if (initialOffset != null) {
			val offsetAnim =
				offsetAnimatable
					?: Animatable(initialOffset!!, Dp.VectorConverter).also {
						offsetAnimatable = it
					}
			
			if (indicatorOffset != offsetAnim.targetValue) {
				coroutineScope.launch { offsetAnim.animateTo(indicatorOffset, animationSpec) }
			}
		} else {
			initialOffset = indicatorOffset
		}
		
		val offset =
			if (layoutDirection == LayoutDirection.Ltr) {
				offsetAnimatable?.value ?: indicatorOffset
			} else {
				-(offsetAnimatable?.value ?: indicatorOffset)
			}
		
		val width = widthAnimatable?.value ?: currentTabWidth
		
		val placeable =
			measurable.measure(
				constraints.copy(minWidth = width.roundToPx(), maxWidth = width.roundToPx())
			)
		
		return layout(placeable.width, placeable.height) { placeable.place(offset.roundToPx(), 0) }
	}
}

internal interface TabPositionsHolder {
	
	fun setTabPositions(positions: List<TabPosition>)
}
