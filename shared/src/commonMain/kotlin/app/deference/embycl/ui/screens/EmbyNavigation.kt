package app.deference.embycl.ui.screens

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.screens.details.EmbyDetailsScreen
import app.deference.embycl.ui.screens.library.EmbyLibraryScreen

fun openItem(item: EmbyItem, backStack: Navigator) {
	if (item.isFolder || item.type in setOf("Series", "Season", "Folder", "CollectionFolder", "BoxSet")) {
		backStack.goTo(EmbyLibraryScreen(item.id, item.name))
	} else {
		backStack.goTo(EmbyDetailsScreen(item.id))
	}
}
