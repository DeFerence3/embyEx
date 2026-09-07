package app.deference.embcl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.ui.screens.EmbyShell
import app.deference.embcl.ui.screens.EmbySignIn
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object EmbyScreen : Screen {

  @Composable
  override fun Content() {
    val sessionStore = koinInject<EmbySessionStore>()
    val session by sessionStore.session.collectAsState()
    AnimatedContent(targetState = session, label = "emby_session") { current ->
      if (current == null) EmbySignIn() else EmbyShell(current)
    }
  }
}
