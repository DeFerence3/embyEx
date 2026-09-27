package app.deference.embycl.ui.screens.signin

import app.deference.embycl.domain.model.EmbyServer
import app.deference.embycl.domain.model.EmbyServerDiscovery
import app.deference.embycl.domain.model.EmbyUser

data class SignInState(
	val server: String = "",
	val username: String = "",
	val password: String = "",
	val isPasswordVisible: Boolean = false,
	val discovery: EmbyServerDiscovery? = null,
	val selectedUser: EmbyUser? = null,
	val isManualSignIn: Boolean = false,
	val isBusy: Boolean = false,
	val error: String? = null,
	val isSearchingLocal: Boolean = false,
	val discoveredServers: List<EmbyServer> = emptyList(),
)
