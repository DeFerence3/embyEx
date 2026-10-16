package app.deference.embycl.core.session

import app.deference.embycl.data.local.preference.EmbyPreference
import app.deference.embycl.domain.model.AuthenticationResult
import app.deference.embycl.domain.model.download.DownloadConnection
import app.deference.embycl.domain.model.download.OfflineAccount
import app.deference.embycl.domain.model.download.OfflineAccountKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

private const val KEY_DEVICE_ID = "device_id"
private const val KEY_LAST_SERVER_URL = "last_server_url"
private const val USERNAME = "username"
private const val ACCESS_TOKEN = "accessToken-key"
private const val USER_ID = "user_id"
private const val SERVER_ID = "server_id"
private const val SERVER_URL = "server-url"
private const val SERVER_NAME = "server-name"
private const val THEME_MODE = "theme_mode"
private const val APP_THEME = "app_theme"

object Session {
	
	lateinit var preferences: EmbyPreference
    private val mutation = Mutex()
	
    private val _browseLocalFiles = MutableStateFlow(false)
    val browseLocalFiles = _browseLocalFiles.asStateFlow()

    private val _downloadConnection = MutableStateFlow<DownloadConnection?>(null)
    val downloadConnection = _downloadConnection.asStateFlow()
	
    private val _offlineAccount = MutableStateFlow<OfflineAccount?>(null)
    val offlineAccount = _offlineAccount.asStateFlow()
	
	val isLoggedInState by lazy { MutableStateFlow(accessToken?.isNotEmpty() == true) }
	
	private val _isConnected = MutableStateFlow(true)
	val isConnected by lazy { _isConnected.asStateFlow() }
	
	fun init(preferences: EmbyPreference) = changeAccount {
		this.preferences = preferences
        _browseLocalFiles.value = false
        publishDownloadConnection()
	}
	
	val deviceId: String
		get() = preferences.getString(KEY_DEVICE_ID) ?: Uuid.random().toString().also {
			preferences.save(KEY_DEVICE_ID, it)
		}
	
	val accessToken: String?
		get() = preferences.getString(ACCESS_TOKEN)
	
	val serverUrl: String
		get() = preferences.getString(SERVER_URL) ?: "http://localhost:8096"
	
	val serverName: String
		get() = if (_browseLocalFiles.value) "Local files" else preferences.getString(SERVER_NAME) ?: "Emby"
	
	val serverId: String
		get() = preferences.getString(SERVER_ID) ?: ""
	
	val userId: String
		get() = preferences.getString(USER_ID) ?: ""
	
	val user: User get() {
		val imageUrl = "$serverUrl/Users/$userId/Images/Primary?MaxWidth=160&Quality=90"
		return User(preferences.getString(USERNAME) ?: "Local files", userId, if (_browseLocalFiles.value || accessToken == null) "" else imageUrl)
	}
	
	fun login(
		userData: AuthenticationResult,
		serverUrl: String,
		serverName: String,
		serverId: String = "",
	) = changeAccount {
		_downloadConnection.value = null
		val effectiveServerId = userData.serverId.ifEmpty { serverId }
        preferences.saveStrings(mapOf(USERNAME to userData.user.name, ACCESS_TOKEN to userData.accessToken,
            USER_ID to userData.user.id, SERVER_ID to effectiveServerId.takeIf { it.isNotEmpty() },
            SERVER_URL to serverUrl, KEY_LAST_SERVER_URL to serverUrl, SERVER_NAME to serverName))
		isLoggedInState.update { true }
		_isConnected.update { true }
        _browseLocalFiles.value = false
        publishDownloadConnection()
	}
	
	fun updateServerUrl(newServerUrl: String, newServerName: String? = null, newServerId: String? = null) = changeAccount {
        _downloadConnection.value = null
        preferences.saveStrings(buildMap {
            put(SERVER_URL, newServerUrl); put(KEY_LAST_SERVER_URL, newServerUrl)
            if (!newServerName.isNullOrEmpty()) put(SERVER_NAME, newServerName)
            if (!newServerId.isNullOrEmpty() && serverId.isEmpty()) put(SERVER_ID, newServerId)
        })
        publishDownloadConnection()
	}
	
	fun logout() = changeAccount {
        _browseLocalFiles.value = false
        _downloadConnection.value = null
        _offlineAccount.value = null
        preferences.saveStrings(listOf(USERNAME, ACCESS_TOKEN, USER_ID, SERVER_ID, SERVER_URL, SERVER_NAME).associateWith { null })
		isLoggedInState.update { false }
	}

    /** Expiry preserves the selected offline account; explicit logout hides it. Ignore stale 401s. */
    fun expireAuthentication(expectedToken: String?) = changeAccount {
        if (expectedToken != null && accessToken == expectedToken) {
            _downloadConnection.value = null
            preferences.saveStrings(mapOf(ACCESS_TOKEN to null))
            isLoggedInState.value = false
        }
    }
	
	fun getLastServerUrl(): String? = preferences.getString(KEY_LAST_SERVER_URL)
	
	private fun safeGet(key: String, default: String = "", run: () -> Unit = ::logout): String = preferences.getString(key) ?: run {
		run()
		default
	}
	
	fun setConnected(bool: Boolean) {
		_isConnected.update { bool }
	}
	
	private fun publishDownloadConnection() {
		val server = preferences.getString(SERVER_ID)?.takeIf { it.isNotBlank() }
		val user = preferences.getString(USER_ID)?.takeIf { it.isNotBlank() }
		val address = preferences.getString(SERVER_URL)?.takeIf { it.isNotBlank() }
		val token = preferences.getString(ACCESS_TOKEN)?.takeIf { it.isNotBlank() }
		_offlineAccount.value = if (server != null && user != null) OfflineAccount(OfflineAccountKey(server, user),
			preferences.getString(SERVER_NAME) ?: "Emby", preferences.getString(USERNAME) ?: "User") else null
		_downloadConnection.value = if (server != null && user != null && address != null && token != null)
			runCatching { DownloadConnection(server, user, address, token) }.getOrNull() else null
	}
	
    fun browseLocalFiles() { _browseLocalFiles.update { true } }
	
    private fun changeAccount(block: () -> Unit) = runBlocking { mutation.withLock { block() } }
}
