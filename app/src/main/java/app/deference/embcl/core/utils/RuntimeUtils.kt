package app.deference.embcl.core.utils

fun Long.asRuntime(): String {
	val totalMinutes = this / 10_000_000L / 60L
	val hours = totalMinutes / 60
	val minutes = totalMinutes % 60
	return if (hours > 0) "$hours h $minutes m" else "$minutes m"
}
