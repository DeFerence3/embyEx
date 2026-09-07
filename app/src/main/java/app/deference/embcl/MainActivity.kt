package app.deference.embcl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.deference.embcl.ui.EmbyScreen
import app.deference.embcl.ui.core.LocalBackStack
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.theme.EmbympvTheme

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			EmbympvTheme {
				EmbympvApp()
			}
		}
	}
}

@Composable
fun EmbympvApp() {
	val backStack = rememberNavBackStack(EmbyScreen)
	CompositionLocalProvider(LocalBackStack provides backStack) {
		NavDisplay(
			backStack = backStack,
			onBack = {
				if (backStack.size > 1) {
					backStack.removeAt(backStack.lastIndex)
				}
			},
			entryProvider = { key ->
				NavEntry(key) {
					CompositionLocalProvider(LocalBackStack provides backStack) {
						(key as? Screen)?.Content()
					}
				}
			}
		)
	}
}