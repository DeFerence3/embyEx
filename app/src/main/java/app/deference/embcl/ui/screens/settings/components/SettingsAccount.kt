package app.deference.embcl.ui.screens.settings.components

import app.deference.embcl.core.session.Session

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