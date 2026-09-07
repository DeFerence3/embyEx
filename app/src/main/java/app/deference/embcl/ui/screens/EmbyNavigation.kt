package app.deference.embcl.ui.screens

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import app.deference.embcl.domain.model.EmbyItem

fun openItem(item: EmbyItem, backStack: NavBackStack<NavKey>) {
	if (item.isFolder || item.type in setOf("Series", "Season", "Folder", "CollectionFolder", "BoxSet")) {
		backStack.add(EmbyLibraryScreen(item.id, item.name))
	} else {
		backStack.add(EmbyDetailsScreen(item.id))
	}
}
