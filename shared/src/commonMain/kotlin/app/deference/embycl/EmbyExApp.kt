package app.deference.embycl

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.repository.download.OfflineReconnection
import app.deference.embycl.ui.EmbyExNav
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.adaptive.ProvideDeviceConfiguration
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.core.network.LocalNetworkPermissionEffect
import app.deference.embycl.ui.core.update.AppUpdateHost
import app.deference.embycl.ui.player.PlayerInlayHost
import app.deference.embycl.ui.screens.downloads.OfflinePlaybackHost
import app.deference.embycl.ui.screens.shell.EmbyShellScreen
import app.deference.embycl.ui.screens.signin.EmbySignInScreen
import com.deference.koasty.KoastyHostConfig
import com.deference.koasty.KoastyProvider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.koinInject


@Composable
fun EmbyExApp(isLoggedIn: Boolean) {
    ProvideDeviceConfiguration {
        EmbyExAppContent(isLoggedIn = isLoggedIn)
    }
}

@Composable
private fun EmbyExAppContent(isLoggedIn: Boolean) {
    val account by Session.offlineAccount.collectAsStateWithLifecycle()
    val localFiles by Session.browseLocalFiles.collectAsStateWithLifecycle()
	val navigator = retain(account?.key, localFiles, isLoggedIn) {
		val startDestination = if (localFiles || account != null || isLoggedIn) EmbyShellScreen else EmbySignInScreen
        Navigator(startDestination)
	}
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && navigator.backStack.lastOrNull() == EmbySignInScreen) {
            navigator.backStack.clear()
            navigator.backStack.add(EmbyShellScreen)
        }
    }
    ServerReconnectionEffect()
	val koastManager = LocalKoasty.current
	val koastyHostConfig = KoastyHostConfig(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        horizontalSwipeEnabled = false
    )
	KoastyProvider(koastManager, config = koastyHostConfig){
		AppUpdateHost()
        PlayerInlayHost {
            EmbyExNav(navigator)
            OfflinePlaybackHost(navigator = navigator)
        }
	}
}

@Composable
private fun ServerReconnectionEffect(
    reconnection: OfflineReconnection = koinInject(),
) {
    LocalNetworkPermissionEffect(onGranted = reconnection::reconnectNow)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(
        reconnection,
        lifecycle,
    ) {
        val foreground = lifecycle.currentStateFlow
            .map { state -> state.isAtLeast(Lifecycle.State.STARTED) }
            .stateIn(scope = this)
        reconnection.run(foreground = foreground)
    }
}
