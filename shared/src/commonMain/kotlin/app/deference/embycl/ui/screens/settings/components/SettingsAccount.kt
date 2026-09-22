package app.deference.embycl.ui.screens.settings.components

import app.deference.embycl.core.session.Session

data class SettingsAccount(
	val username: String,
	val userId: String,
	val serverName: String,
	val serverUrl: String
){
	companion object {
		fun getFromSession() = SettingsAccount(Session.user.name, Session.user.id, Session.serverName, Session.serverUrl)
	}
}