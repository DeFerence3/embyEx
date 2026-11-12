package app.deference.embycl.ui.screens.settings.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.screens.settings.home.components.HomeLayoutSettingsContent
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** Settings › Edit Home: what Home shows, in which order, and how big its cards are. */
@Serializable
data object HomeLayoutSettingsScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel = koinViewModel<HomeLayoutSettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        HomeLayoutSettingsContent(
            state = state,
            onAction = viewModel::onAction,
            onBack = navigator::goBack,
        )
    }
}
