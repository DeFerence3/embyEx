package app.deference.embycl.ui.screens.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.Home as HomeIcon

enum class EmbyTab {
	Home,
	Libraries,
	Search;
	
	val selectedIcon get() = when(this){
		Home -> Icons.Filled.HomeIcon
		Libraries -> Icons.Filled.VideoLibrary
		Search -> Icons.Filled.Search
	}
	
	val unselectedIcon get() = when(this){
		Home -> Icons.Outlined.Home
		Libraries -> Icons.Outlined.VideoLibrary
		Search -> Icons.Outlined.Search
	}
	
	val icon get() = selectedIcon
	
	val label get() = when(this){
		Home -> "Home"
		Libraries -> "Libraries"
		Search -> "Search"
	}
	
}