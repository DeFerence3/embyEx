package app.deference.embycl.ui.screens

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.ui.core.nav.Navigator
import app.deference.embycl.ui.screens.details.EmbyDetailsScreen
import app.deference.embycl.ui.screens.library.EmbyLibraryScreen

fun openItem(item: EmbyItem, backStack: Navigator) {
	backStack.openItem(
		id = item.id,
		name = item.name,
		type = item.type,
		isFolder = item.isFolder
	)
}

fun Navigator.openItem(
	id: String,
	name: String,
	type: String,
	isFolder: Boolean
) {
	if (isFolder || type in setOf("Series", "Season", "Folder", "CollectionFolder", "BoxSet")) {
		this.goTo(EmbyLibraryScreen(id, name))
	} else {
		this.goTo(EmbyDetailsScreen(id))
	}
}
