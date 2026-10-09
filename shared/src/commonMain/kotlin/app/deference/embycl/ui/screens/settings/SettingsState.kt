package app.deference.embycl.ui.screens.settings

import app.deference.embycl.domain.model.ServerDetails

data class SettingsState(
	val isLoading: Boolean = false,
	val serverDetails: ServerDetails? = null,
	val error: String? = null,
	val changelog: Result<String>? = null,
	val isChangelogLoading: Boolean = false,
)
