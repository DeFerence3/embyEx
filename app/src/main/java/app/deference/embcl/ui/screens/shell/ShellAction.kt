package app.deference.embcl.ui.screens.shell

sealed interface ShellAction {
	data class SelectTab(val tab: EmbyTab) : ShellAction
	data class SetAccountMenuOpen(val isOpen: Boolean) : ShellAction
	data object SignOut : ShellAction
}
