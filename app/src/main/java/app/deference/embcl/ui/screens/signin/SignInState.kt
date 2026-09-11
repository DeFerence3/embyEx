package app.deference.embcl.ui.screens.signin

import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUdpServer
import app.deference.embcl.domain.model.EmbyUser

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
	val discoveredServers: List<EmbyUdpServer> = emptyList(),
)
