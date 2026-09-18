package app.deference.embcl.core.utils

const val isLogging = true
const val TAG = "Emby-Mpv"

object Log {
	
	fun i(tag: String = TAG, message: () -> String) = if (isLogging) {
		logPlatform("INFO", tag, message())
	} else Unit
	
	fun d(tag: String = TAG, message: () -> String) = if (isLogging) {
		logPlatform("DEBUG", tag, message())
	} else Unit
	
	fun w(tag: String = TAG, message: () -> String) = if (isLogging) {
		logPlatform("WARN", tag, message())
	} else Unit
	
	fun raw(message: () -> String) = if (isLogging) {
		logPlatform("RAW", TAG, message())
	} else Unit
}

internal fun logPlatform(level: String, tag: String, message: String) {
	when (level) {
		"WARN" -> android.util.Log.w(tag, message)
		"INFO" -> android.util.Log.i(tag, message)
		"DEBUG" -> android.util.Log.d(tag, message)
		"RAW" -> android.util.Log.i(tag, message)
		else -> android.util.Log.i(tag, message)
	}
}