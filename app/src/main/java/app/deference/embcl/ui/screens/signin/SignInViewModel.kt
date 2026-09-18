package app.deference.embcl.ui.screens.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.core.networking.DataState.Loading.onFailure
import app.deference.embcl.core.networking.DataState.Loading.onSuccess
import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.toUrl
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.AuthRepo
import app.deference.embcl.domain.repository.ServerFinderRepo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SignInViewModel(
	private val authRepo: AuthRepo,
	private val serverFinderRepo: ServerFinderRepo,
) : ViewModel() {
	
	private val _state = MutableStateFlow(SignInState())
	val state = _state.asStateFlow()
	private val _events = Channel<SignInEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()
	
	init {
		scanLocalServers()
	}
	
	fun onAction(action: SignInAction) {
		when (action) {
			is SignInAction.ServerChanged -> update { it.copy(server = action.value) }
			is SignInAction.UsernameChanged -> update { it.copy(username = action.value) }
			is SignInAction.PasswordChanged -> update { it.copy(password = action.value) }
			SignInAction.TogglePasswordVisibility -> update { it.copy(isPasswordVisible = ! it.isPasswordVisible) }
			SignInAction.DiscoverServer -> discoverServer(_state.value.server)
			is SignInAction.SelectServer -> {
				update { it.copy(server = action.address) }
				discoverServer(action.address)
			}
			
			SignInAction.ScanLocalServers -> scanLocalServers()
			is SignInAction.SelectUser -> selectUser(action.user)
			SignInAction.SignInSelectedUser -> _state.value.selectedUser?.let { authenticate(it, _state.value.password) }
			SignInAction.SignInManually -> authenticateManually()
			SignInAction.ShowManualSignIn -> update { it.copy(isManualSignIn = true, error = null, password = "") }
			SignInAction.ShowPublicUsers -> update { it.copy(isManualSignIn = false, error = null, password = "") }
			SignInAction.ChooseAnotherUser -> update { it.copy(selectedUser = null, error = null, password = "") }
			SignInAction.ChangeServer -> update {
				it.copy(discovery = null, selectedUser = null, isManualSignIn = false, error = null, password = "")
			}
			
			SignInAction.UseLocalFiles -> viewModelScope.launch { /*_events.send(SignInEvent.OpenLocalFiles)*/ }
		}
	}
	
	private fun discoverServer(address: String) {
		if (_state.value.isBusy || address.isBlank()) return
		update { it.copy(isBusy = true, error = null) }
		viewModelScope.launch {
			runCatching { serverFinderRepo.discoverServer(address) }
				.onSuccess { discovery ->
					update {
						it.copy(
							server = address,
							discovery = discovery,
							selectedUser = null,
							isManualSignIn = false,
							password = "",
							isBusy = false,
						)
					}
				}
				.onFailure { fail(it, "Could not connect to the Emby server.") }
		}
	}
	
	private fun scanLocalServers() {
		if (_state.value.isSearchingLocal) return
		update { it.copy(isSearchingLocal = true) }
		viewModelScope.launch {
			val local = runCatching { serverFinderRepo.searchForLocallyRunningServers() }.getOrDefault(emptyList())
			val saved = Session.getLastServerUrl()
			update { it.copy(discoveredServers = local, isSearchingLocal = false) }
			if (local.size == 1) {
				val target = local.firstOrNull()
				if (target != null) {
					update { it.copy(server = target.address) }
					discoverServer(target.address)
				}
			} else if (saved != null) {
				update { it.copy(server = saved) }
				discoverServer(saved)
			}
		}
	}
	
	private fun selectUser(user: EmbyUser) {
		update { it.copy(password = "", error = null) }
		if (user.hasPassword || user.hasConfiguredPassword) {
			update { it.copy(selectedUser = user) }
		} else {
			authenticate(user, "")
		}
	}
	
	private fun authenticateManually() {
		val discovery = _state.value.discovery ?: return
		if (_state.value.isBusy || _state.value.username.isBlank()) return
		update { it.copy(isBusy = true, error = null) }
		viewModelScope.launch {
			authRepo.authenticate(discovery, _state.value.username, _state.value.password)
				.onSuccess { result ->
					val server = discovery.server
					Session.login(result, server.host.toUrl(server.port, server.scheme), discovery.serverInfo.serverName)
					update { it.copy(isBusy = false) }
				}
				.onFailure { fail(it) }
		}
	}
	
	private fun authenticate(user: EmbyUser, password: String) {
		val discovery = _state.value.discovery ?: return
		if (_state.value.isBusy) return
		update { it.copy(selectedUser = user, isBusy = true, error = null) }
		viewModelScope.launch {
			authRepo.authenticate(discovery, user, password)
				.onSuccess { result ->
					val server = discovery.server
					Session.login(result, server.host.toUrl(server.port, server.scheme), discovery.serverInfo.serverName)
					update { it.copy(isBusy = false) }
				}
				.onFailure { fail(it) }
		}
	}
	
	private suspend fun fail(throwable: Throwable, fallback: String) {
		throwable.printStackTrace()
		val message = throwable.message ?: fallback
		update { it.copy(isBusy = false, error = message) }
		_events.send(SignInEvent.Error(message))
	}
	
	private suspend fun fail(message: String) {
		update { it.copy(isBusy = false, error = message) }
		_events.send(SignInEvent.Error(message))
	}
	
	private inline fun update(transform: (SignInState) -> SignInState) {
		_state.value = transform(_state.value)
	}
}
