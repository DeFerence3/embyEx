package app.deference.embcl.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.nav.Navigator
import kotlinx.serialization.Serializable

val LocalBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
	error("No backstack provided")
}

@Serializable
object MainScreen : Screen {
	
	@Composable
	override fun Content() {
	}
}
