package app.deference.embcl.ui.screens.shell

enum class EmbyTab { Home, Libraries, Search }

data class ShellState(
	val selectedTab: EmbyTab = EmbyTab.Home,
	val isAccountMenuOpen: Boolean = false,
)
