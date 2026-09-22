package app.deference.embycl.ui.screens.libraries

import app.deference.embycl.domain.model.EmbyItem

data class LibrariesState(val content: Result<List<EmbyItem>>? = null)
