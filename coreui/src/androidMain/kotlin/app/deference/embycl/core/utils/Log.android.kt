package app.deference.embycl.core.utils

internal actual fun logPlatform(level: String, tag: String, message: String) {
	when (level) {
		"WARN" -> android.util.Log.w(tag, message)
		"INFO" -> android.util.Log.i(tag, message)
		"DEBUG" -> android.util.Log.d(tag, message)
		"RAW" -> android.util.Log.i(tag, message)
		else -> android.util.Log.i(tag, message)
	}
}