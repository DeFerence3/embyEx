package app.deference.embycl.ui.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.deference.embycl.core.session.Session
import app.deference.embycl.ui.player.PlayerSubtitleAlignment
import app.deference.embycl.ui.player.PlayerSubtitleBackground
import app.deference.embycl.ui.player.PlayerSubtitleStyle

/** Persisted UI toggles. Both are shown by default. */
object SettingsPreferences {
	private const val KEY_BANNERS_HIDDEN = "banners_hidden"
	private const val KEY_TITLE_BAR_HIDDEN = "title_bar_hidden"
	private const val KEY_BETA_UPDATES = "beta_updates_enabled"
	private const val KEY_AUTOPLAY_NEXT = "autoplay_next_episode"
	private const val KEY_SUBTITLE_SIZE = "subtitle_size_percent"
	private const val KEY_SUBTITLE_BACKGROUND = "subtitle_background"
	private const val KEY_SUBTITLE_VERTICAL = "subtitle_vertical_percent"
	private const val KEY_SUBTITLE_HORIZONTAL = "subtitle_horizontal_percent"
	private const val KEY_SUBTITLE_ALIGNMENT = "subtitle_alignment"

	var bannerEnabled: Boolean by mutableStateOf(load(KEY_BANNERS_HIDDEN))
		private set

	/** Desktop only: draw the app title bar instead of the system window frame. */
	var titleBarEnabled: Boolean by mutableStateOf(load(KEY_TITLE_BAR_HIDDEN))
		private set

	/** Opt-in to pre-release app updates; off by default. */
	var betaUpdatesEnabled: Boolean by mutableStateOf(load(KEY_BETA_UPDATES,false))
		private set

	/** Start the next episode when one ends; on by default. Set from the player's Up next panel. */
	var autoplayNextEnabled: Boolean by mutableStateOf(load(KEY_AUTOPLAY_NEXT))
		private set

	/** How text subtitles look in the player; set from its Subtitle style page. */
	var subtitleStyle: PlayerSubtitleStyle by mutableStateOf(loadSubtitleStyle())
		private set

	fun updateSubtitleStyle(value: PlayerSubtitleStyle) {
		subtitleStyle = value
		runCatching {
			Session.preferences.saveStrings(
				values = mapOf(
					KEY_SUBTITLE_SIZE to value.sizePercent.toString(),
					KEY_SUBTITLE_BACKGROUND to value.background.name,
					KEY_SUBTITLE_VERTICAL to value.verticalPercent.toString(),
					KEY_SUBTITLE_HORIZONTAL to value.horizontalPercent.toString(),
					KEY_SUBTITLE_ALIGNMENT to value.alignment.name,
				),
			)
		}
	}

	fun setAutoplayNext(value: Boolean) {
		autoplayNextEnabled = value
		runCatching { Session.preferences.save(KEY_AUTOPLAY_NEXT, value) }
	}

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

	/** Unknown or missing values (an older or newer app version) fall back to the default per field; numbers out of range are clamped. */
	private fun loadSubtitleStyle(): PlayerSubtitleStyle {
		val default = PlayerSubtitleStyle.Default
		fun string(key: String): String? = runCatching { Session.preferences.getString(key) }.getOrNull()
		return PlayerSubtitleStyle(
			sizePercent = string(KEY_SUBTITLE_SIZE)?.toIntOrNull() ?: default.sizePercent,
			background = PlayerSubtitleBackground.entries.firstOrNull { it.name == string(KEY_SUBTITLE_BACKGROUND) } ?: default.background,
			verticalPercent = string(KEY_SUBTITLE_VERTICAL)?.toIntOrNull() ?: default.verticalPercent,
			horizontalPercent = string(KEY_SUBTITLE_HORIZONTAL)?.toIntOrNull() ?: default.horizontalPercent,
			alignment = PlayerSubtitleAlignment.entries.firstOrNull { it.name == string(KEY_SUBTITLE_ALIGNMENT) } ?: default.alignment,
		).coerced()
	}

	private fun load(hiddenKey: String, defaultValue: Boolean = true): Boolean =
		runCatching { Session.preferences.getBoolean(hiddenKey) }.getOrDefault(defaultValue)
}
