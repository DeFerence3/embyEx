package app.deference.embycl.ui.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.deference.embycl.core.session.Session

/** Persisted UI toggles. Both are shown by default. */
object SettingsPreferences {
	private const val KEY_BANNERS_HIDDEN = "banners_hidden"
	private const val KEY_TITLE_BAR_HIDDEN = "title_bar_hidden"
	private const val KEY_BETA_UPDATES = "beta_updates_enabled"

	var bannerEnabled: Boolean by mutableStateOf(load(KEY_BANNERS_HIDDEN))
		private set

	/** Desktop only: draw the app title bar instead of the system window frame. */
	var titleBarEnabled: Boolean by mutableStateOf(load(KEY_TITLE_BAR_HIDDEN))
		private set

	/** Opt-in to pre-release app updates; off by default. */
	var betaUpdatesEnabled: Boolean by mutableStateOf(load(KEY_BETA_UPDATES,false))
		private set

	fun setBetaUpdates(value: Boolean) {
		betaUpdatesEnabled = value
		runCatching { Session.preferences.save(KEY_BETA_UPDATES,value) }
	}

	fun setBanner(value: Boolean) {
		bannerEnabled = value
		runCatching { Session.preferences.save(KEY_BANNERS_HIDDEN, value) }
	}

	fun setTitleBar(value: Boolean) {
		titleBarEnabled = value
		runCatching { Session.preferences.save(KEY_TITLE_BAR_HIDDEN, value) }
	}

	private fun load(hiddenKey: String, defaultValue: Boolean = true): Boolean =
		runCatching { Session.preferences.getBoolean(hiddenKey) }.getOrDefault(defaultValue)
}
