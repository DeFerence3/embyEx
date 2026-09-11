package app.deference.embcl.ui.screens.signin

sealed interface SignInEvent {
	data class Error(val message: String) : SignInEvent
}
