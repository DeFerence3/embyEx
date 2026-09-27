package app.deference.embycl.core.utils

internal actual fun logPlatform(level: String, tag: String, message: String) {
	if (level == "RAW") println(message) else println("$level: $tag  $message")
}