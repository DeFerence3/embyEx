package app.deference.embycl.ui.screens.home

import app.deference.embycl.domain.model.EmbyHome

data class HomeState(val content: Result<EmbyHome>? = null)
