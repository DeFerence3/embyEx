package app.deference.embcl.core.session

import android.content.Context
import androidx.core.content.edit
import app.deference.embcl.domain.model.EmbySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.UUID

class EmbySessionStore(context: Context) {
  private val preferences = context.getSharedPreferences("emby_session", Context.MODE_PRIVATE)
  private val json = Json { ignoreUnknownKeys = true }
  private val _session = MutableStateFlow(readSession())

  val session = _session.asStateFlow()

  fun deviceId(): String = preferences.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
    preferences.edit { putString(KEY_DEVICE_ID, it) }
  }

  fun save(session: EmbySession) {
    preferences.edit {
      putString(KEY_SESSION, json.encodeToString(EmbySession.serializer(), session))
      putString(KEY_LAST_SERVER_URL, session.serverUrl)
    }
    _session.value = session
  }

  fun saveLastServerUrl(url: String) {
    preferences.edit { putString(KEY_LAST_SERVER_URL, url) }
  }

  fun getLastServerUrl(): String? = preferences.getString(KEY_LAST_SERVER_URL, null)

  fun clear() {
    preferences.edit().remove(KEY_SESSION).apply()
    _session.value = null
  }

  private fun readSession(): EmbySession? = preferences.getString(KEY_SESSION, null)?.let { stored ->
    runCatching { json.decodeFromString(EmbySession.serializer(), stored) }.getOrNull()
  }

  private companion object {
    const val KEY_SESSION = "session"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_LAST_SERVER_URL = "last_server_url"
  }
}
