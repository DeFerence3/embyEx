package app.deference.embcl.ui.screens.settings

import app.deference.embcl.domain.model.ServerDetails

data class SettingsState(
	val isLoading: Boolean = false,
	val serverDetails: ServerDetails? = null,
	val error: String? = null,
)