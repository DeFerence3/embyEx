package app.deference.embycl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.EmbyExNav
import app.deference.embycl.ui.NotConnected
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.update.AppUpdateHost
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.screens.shell.EmbyShellScreen
import app.deference.embycl.ui.screens.signin.EmbySignInScreen
import com.deference.koasty.KoastyProvider

@Composable
fun EmbyExApp(isLoggedIn: Boolean) {
	val navigator = retain(isLoggedIn) {
		val startDestination = if (isLoggedIn) EmbyShellScreen else EmbySignInScreen
		Navigator(startDestination)
	}
	val koastManager = LocalKoasty.current
	KoastyProvider(koastManager){
		AppUpdateHost()
		val connected by Session.isConnected.collectAsStateWithLifecycle()
		if (connected) {
			EmbyExNav(navigator)
		}else{
			NotConnected()
		}
	}
}
