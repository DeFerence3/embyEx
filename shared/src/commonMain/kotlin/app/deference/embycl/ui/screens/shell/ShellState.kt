package app.deference.embycl.ui.screens.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Home as HomeIcon

enum class EmbyTab {
	Home,
	Libraries,
	Search;
	
	val icon get() = when(this){
		Home -> Icons.Filled.HomeIcon
		Libraries -> Icons.Filled.VideoLibrary
		Search -> Icons.Filled.Search
	}
	
	val label get() = when(this){
		Home -> "Home"
		Libraries -> "Libraries"
		Search -> "Search"
	}

}

data class ShellState(
	val selectedTab: EmbyTab = EmbyTab.Home,
	val isAccountMenuOpen: Boolean = false,
)
