package app.deference.embcl.ui.core.nav

import androidx.compose.runtime.mutableStateListOf
import app.deference.embcl.ui.Screen

class Navigator(startDestination: Screen) {
	val backStack = mutableStateListOf(startDestination)
	
	fun goTo(destination: Screen) {
		backStack.add(destination)
	}
	
	fun goBack() {
		backStack.removeLastOrNull()
	}
}