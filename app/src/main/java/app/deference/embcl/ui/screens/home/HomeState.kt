package app.deference.embcl.ui.screens.home

import app.deference.embcl.domain.model.EmbyHome

data class HomeState(val content: Result<EmbyHome>? = null)
