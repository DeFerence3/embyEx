package app.deference.embycl

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.v2.Window
import androidx.compose.ui.window.v2.rememberWindowState
import app.deference.embycl.core.di.initKoin
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.preference.EmbyPreference
import app.deference.embycl.data.preference.createDataStore
import app.deference.embycl.ui.theme.EmbympvTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
	initKoin()
	val dataStore = createDataStore()
	Session.init(EmbyPreference(dataStore))
	val state = rememberWindowState(initialPlacement = WindowPlacement.Maximized)
    Window(
        onCloseRequest = ::exitApplication,
        title = "embycl",
		state = state
    ) {
		EmbympvTheme {
			val isLoggedIn by Session.isLoggedInState.collectAsState()
			EmbyExApp(isLoggedIn)
		}
    }
}
