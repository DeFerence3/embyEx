package app.deference.embcl.core.session

import app.deference.embcl.data.preference.EmbyPreference
import app.deference.embcl.domain.model.EmbySession
import java.util.UUID

private const val KEY_SESSION = "session"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_LAST_SERVER_URL = "last_server_url"

object Session {
	
	lateinit var preferences: EmbyPreference
	
	private var session: EmbySession? = null
	
	fun init(preferences: EmbyPreference) {
		this.preferences = preferences
	}
	
	fun deviceId(): String = preferences.getString(KEY_DEVICE_ID) ?: UUID.randomUUID().toString().also {
		preferences.save(KEY_DEVICE_ID, it)
	}
	
	
}