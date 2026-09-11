package app.deference.embcl.ui.screens.signin

import app.deference.embcl.domain.model.EmbyUser

sealed interface SignInAction {
	data class ServerChanged(val value: String) : SignInAction
	data class UsernameChanged(val value: String) : SignInAction
	data class PasswordChanged(val value: String) : SignInAction
	data object TogglePasswordVisibility : SignInAction
	data object DiscoverServer : SignInAction
	data class SelectServer(val address: String) : SignInAction
	data object ScanLocalServers : SignInAction
	data class SelectUser(val user: EmbyUser) : SignInAction
	data object SignInSelectedUser : SignInAction
	data object SignInManually : SignInAction
	data object ShowManualSignIn : SignInAction
	data object ShowPublicUsers : SignInAction
	data object ChooseAnotherUser : SignInAction
	data object ChangeServer : SignInAction
	data object UseLocalFiles : SignInAction
}
