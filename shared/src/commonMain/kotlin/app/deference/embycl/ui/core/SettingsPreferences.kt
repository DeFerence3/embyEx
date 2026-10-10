package app.deference.embycl.ui.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.deference.embycl.core.session.Session

/** Persisted toggle for page intro banners. Shown by default. */
object SettingsPreferences {
	private const val KEY_BANNERS_HIDDEN = "banners_hidden"

	var bannerEnabled: Boolean by mutableStateOf(load())
		private set

	fun setBanner(value: Boolean) {
		bannerEnabled = value
		runCatching { Session.preferences.save(KEY_BANNERS_HIDDEN, !value) }
	}

	private fun load(): Boolean =
		runCatching { !Session.preferences.getBoolean(KEY_BANNERS_HIDDEN) }.getOrDefault(true)
}
