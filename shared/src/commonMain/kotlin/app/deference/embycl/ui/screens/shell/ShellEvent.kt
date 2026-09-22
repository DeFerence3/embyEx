package app.deference.embycl.ui.screens.shell

sealed interface ShellEvent {
	data object SignedOut : ShellEvent
}
