package app.deference.embcl.ui.screens

import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.ui.core.nav.Navigator
import app.deference.embcl.ui.screens.details.EmbyDetailsScreen
import app.deference.embcl.ui.screens.library.EmbyLibraryScreen

fun openItem(item: EmbyItem, backStack: Navigator) {
	if (item.isFolder || item.type in setOf("Series", "Season", "Folder", "CollectionFolder", "BoxSet")) {
		backStack.goTo(EmbyLibraryScreen(item.id, item.name))
	} else {
		backStack.goTo(EmbyDetailsScreen(item.id))
	}
}
