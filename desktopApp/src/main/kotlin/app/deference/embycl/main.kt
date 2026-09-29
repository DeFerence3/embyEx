package app.deference.embycl

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.rememberWindowState
import app.deference.embycl.core.di.initKoin
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.preference.EmbyPreference
import app.deference.embycl.data.preference.createDataStore
import app.deference.embycl.ui.core.ExitConfirmationDialog
import app.deference.embycl.ui.theme.EmbympvTheme
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.window.material.MaterialDecoratedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() = nucleusApplication {
	initKoin()
	val dataStore = createDataStore()
	Session.init(EmbyPreference(dataStore))
	val state = rememberWindowState()
	var isLogoutRequested by remember { mutableStateOf(false) }
	MaterialDecoratedWindow(
		onCloseRequest = { isLogoutRequested = true },
		title = "embyEx",
		state = state
	) {
		EmbympvTheme(
			darkTheme = isSystemInDarkMode()
		) {
			if (isLogoutRequested){
				ExitConfirmationDialog(
					onConfirm = ::exitApplication,
					onDismiss = { isLogoutRequested = false }
				)
			}
			val isLoggedIn by Session.isLoggedInState.collectAsState()
			EmbyExTitleBar()
			EmbyExApp(isLoggedIn)
		}
	}
}
