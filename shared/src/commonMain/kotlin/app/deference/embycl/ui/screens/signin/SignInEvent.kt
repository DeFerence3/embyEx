package app.deference.embycl.ui.screens.signin

sealed interface SignInEvent {
	data class Error(val message: String) : SignInEvent
}
