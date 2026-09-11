package app.deference.embcl.ui.screens.libraries

import app.deference.embcl.domain.model.EmbyItem

data class LibrariesState(val content: Result<List<EmbyItem>>? = null)
