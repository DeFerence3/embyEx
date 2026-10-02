package app.deference.embycl.ui.screens.shell

import androidx.lifecycle.ViewModel
import app.deference.embycl.core.session.Session
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ShellViewModel : ViewModel() {
    private val _state = MutableStateFlow(ShellState())
    val state = _state.asStateFlow()
    private val _events = Channel<ShellEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: ShellAction) {
        when (action) {
            is ShellAction.SelectTab -> _state.update { it.copy(selectedTab = action.tab) }
            is ShellAction.SetAccountMenuOpen -> _state.update { it.copy(isAccountMenuOpen = action.isOpen) }
            ShellAction.SignOut -> {
                _state.update { it.copy(isAccountMenuOpen = false) }
                Session.logout()
                _events.trySend(ShellEvent.SignedOut)
            }
        }
    }
}
