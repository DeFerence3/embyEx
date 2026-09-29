package app.deference.embycl

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowPlacement
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
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.app_name
import embyex.shared.generated.resources.embyex_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
fun main() = nucleusApplication {
	initKoin()
	val dataStore = createDataStore()
	Session.init(EmbyPreference(dataStore))
	val state = rememberWindowState(placement = WindowPlacement.Maximized)
	var isLogoutRequested by remember { mutableStateOf(false) }
	MaterialDecoratedWindow(
		state = state,
		onCloseRequest = { isLogoutRequested = true },
		title = stringResource(Res.string.app_name),
		icon = painterResource(Res.drawable.embyex_logo)
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
