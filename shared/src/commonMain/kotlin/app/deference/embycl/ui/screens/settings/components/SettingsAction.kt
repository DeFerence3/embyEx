package app.deference.embycl.ui.screens.settings.components

data class SettingsAction(
	val header: String,
	val description: String,
	val value: Boolean,
	val onAction: (Boolean) -> Unit,
)
