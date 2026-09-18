package app.deference.embcl.core.session

import app.deference.embcl.data.preference.EmbyPreference
import app.deference.embcl.domain.model.AuthenticationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

private const val KEY_DEVICE_ID = "device_id"
private const val KEY_LAST_SERVER_URL = "last_server_url"
private const val USERNAME = "username"
private const val ACCESS_TOKEN = "accessToken-key"
private const val USER_ID = "user_id"
private const val SERVER_URL = "server-url"
private const val SERVER_NAME = "server-name"
private const val THEME_MODE = "theme_mode"
private const val APP_THEME = "app_theme"

object Session {
	
	lateinit var preferences: EmbyPreference
	val isLoggedInState by lazy { MutableStateFlow(accessToken?.isNotEmpty() == true) }
	
	fun init(preferences: EmbyPreference) {
		this.preferences = preferences
	}
	
	val deviceId: String
		get() = preferences.getString(KEY_DEVICE_ID) ?: Uuid.random().toString().also {
			preferences.save(KEY_DEVICE_ID, it)
		}
	
	val accessToken: String?
		get() = preferences.getString(ACCESS_TOKEN)
	
	val serverUrl: String
		get() = safeGet(SERVER_URL, default = "http://localhost:8096")
	
	val serverName: String
		get() = safeGet(SERVER_NAME)
	
	val userId: String
		get() = safeGet(USER_ID)
	
	val user: User get() {
		val imageUrl = "$serverUrl/Users/$userId/Images/Primary?MaxWidth=160&Quality=90"
		return User(safeGet(USERNAME), safeGet(USER_ID),imageUrl)
	}
	
	fun login(
		userData: AuthenticationResult,
		serverUrl: String,
		serverName: String,
	) {
		preferences.save(USERNAME, userData.user.name)
		preferences.save(ACCESS_TOKEN, userData.accessToken)
		preferences.save(USER_ID, userData.user.id)
		
		preferences.save(SERVER_URL, serverUrl)
		preferences.save(KEY_LAST_SERVER_URL, serverUrl)
		preferences.save(SERVER_NAME, serverName)
		isLoggedInState.update { true }
	}
	
	fun logout() {
		preferences.remove(USERNAME)
		preferences.remove(ACCESS_TOKEN)
		preferences.remove(USER_ID)
		
		preferences.remove(SERVER_URL)
		preferences.remove(SERVER_NAME)
		isLoggedInState.update { false }
	}
	
	fun getLastServerUrl(): String? = preferences.getString(KEY_LAST_SERVER_URL)
	
	private fun safeGet(key: String, default: String = "", run: () -> Unit = ::logout): String = preferences.getString(key) ?: run {
		run()
		default
	}
}