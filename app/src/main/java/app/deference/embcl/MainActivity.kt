package app.deference.embcl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.ui.core.LocalNavigator
import app.deference.embcl.ui.core.nav.Navigator
import app.deference.embcl.ui.screens.shell.EmbyShellScreen
import app.deference.embcl.ui.screens.signin.EmbySignInScreen
import app.deference.embcl.ui.theme.EmbympvTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			EmbympvTheme {
				val sessionStore = koinInject<EmbySessionStore>()
				val session by sessionStore.session.collectAsState()
				Surface{
					EmbympvApp(session)
				}
			}
		}
	}
}

@Composable
fun EmbympvApp(session: EmbySession?) {
	val navigator = retain(session) {
		val startDestination = if (session != null) EmbyShellScreen(session) else EmbySignInScreen
		Navigator(startDestination)
	}
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
						fadeIn(animationSpec = tween(220)) +
								slideIn(animationSpec = tween(220)) { IntOffset(- it.width / 2, 0) }
						) togetherWith (
						fadeOut(animationSpec = tween(220)) +
								slideOut(animationSpec = tween(220)) { IntOffset(it.width / 2, 0) }
						)
			},
			transitionSpec = {
				(
						fadeIn(animationSpec = tween(220)) +
								slideIn(animationSpec = tween(220)) { IntOffset(it.width / 2, 0) }
						) togetherWith (
						fadeOut(animationSpec = tween(220)) +
								slideOut(animationSpec = tween(220)) { IntOffset(- it.width / 2, 0) }
						)
			},
			predictivePopTransitionSpec = {
				(
						fadeIn(animationSpec = tween(220)) +
								scaleIn(
									animationSpec = tween(220, delayMillis = 30),
									initialScale = .9f,
									TransformOrigin(- 1f, .5f),
								)
						) togetherWith (
						fadeOut(animationSpec = tween(220)) +
								scaleOut(
									animationSpec = tween(220, delayMillis = 30),
									targetScale = .9f,
									TransformOrigin(- 1f, .5f),
								)
						)
			},
		)
	}
}
