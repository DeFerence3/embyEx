package app.deference.embycl.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.nav.Navigator

@Composable
fun EmbyExNav(navigator: Navigator) {
	val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
	val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
	CompositionLocalProvider(LocalNavigator provides navigator) {
		NavDisplay(
			backStack = navigator.backStack,
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
			),
			onBack = { navigator.goBack() },
			entryProvider = { key ->
				NavEntry(key) {
					key.Content()
				}
			},
			popTransitionSpec = {
				(
						fadeIn(animationSpec = effects) +
								slideIn(animationSpec = spatial) { IntOffset(- it.width / 12, 0) }
						) togetherWith (
						fadeOut(animationSpec = effects) +
								slideOut(animationSpec = spatial) { IntOffset(it.width / 12, 0) }
						)
			},
			transitionSpec = {
				(
						fadeIn(animationSpec = effects) +
								slideIn(animationSpec = spatial) { IntOffset(it.width / 12, 0) }
						) togetherWith (
						fadeOut(animationSpec = effects) +
								slideOut(animationSpec = spatial) { IntOffset(- it.width / 12, 0) }
						)
			},
			predictivePopTransitionSpec = {
				(
						fadeIn(animationSpec = effects) +
								scaleIn(
									animationSpec = effects,
									initialScale = .9f,
									TransformOrigin(- 1f, .5f),
								)
						) togetherWith (
						fadeOut(animationSpec = effects) +
								scaleOut(
									animationSpec = effects,
									targetScale = .9f,
									TransformOrigin(- 1f, .5f),
								)
						)
			},
		)
	}
}
