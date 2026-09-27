package app.deference.embycl.core.utils

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

internal expect fun logPlatform(level: String, tag: String, message: String)