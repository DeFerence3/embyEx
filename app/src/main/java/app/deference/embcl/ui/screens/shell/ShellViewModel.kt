package app.deference.embcl.ui.screens.shell

import androidx.lifecycle.ViewModel
import app.deference.embcl.core.session.Session
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ShellViewModel: ViewModel() {
	
	private val _state = MutableStateFlow(ShellState())
	val state = _state.asStateFlow()
	private val _events = Channel<ShellEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	
	fun onAction(action: ShellAction) {
		when (action) {
			is ShellAction.SelectTab -> _state.value = _state.value.copy(selectedTab = action.tab)
			is ShellAction.SetAccountMenuOpen -> _state.value = _state.value.copy(isAccountMenuOpen = action.isOpen)
			ShellAction.SignOut -> {
				_state.value = _state.value.copy(isAccountMenuOpen = false)
				Session.logout()
				_events.trySend(ShellEvent.SignedOut)
			}
		}
	}
}
