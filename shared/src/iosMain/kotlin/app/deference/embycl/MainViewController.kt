package app.deference.embycl

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import app.deference.embycl.core.session.Session
import app.deference.embycl.theme.EmbympvTheme

fun MainViewController() = ComposeUIViewController {
    EmbympvTheme {
        val isLoggedIn by Session.isLoggedInState.collectAsState()
        EmbyExApp(isLoggedIn = isLoggedIn)
    }
}
