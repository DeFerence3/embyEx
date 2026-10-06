package app.deference.embycl.ui.screens.shell

data class ShellState(
	val selectedTab: EmbyTab = EmbyTab.Home,
	val isAccountMenuOpen: Boolean = false,
)
