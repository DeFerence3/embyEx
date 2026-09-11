package app.deference.embcl.ui.screens.shell

sealed interface ShellEvent {
	data object SignedOut : ShellEvent
}
