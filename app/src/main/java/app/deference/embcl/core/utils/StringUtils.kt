package app.deference.embcl.core.utils

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

fun <T> T?.or(predicate: () -> T): T = this ?: predicate()

fun <T> T?.or(predicate: () -> String): String = this?.toString() ?: predicate()

/**
 * converts any string into a url format (appends https and trims '/' or any characters)
 */
fun String.toUrl(): String {
	val trimmed = this.trim().trimEnd('/')
	require(trimmed.isNotBlank()) { "Not a valid url or a empty string." }
	val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
	val parsed = withScheme.toHttpUrlOrNull() ?: throw IllegalArgumentException("Enter a valid server address.")
	return parsed.toString().trimEnd('/')
}

fun String.toUrl(port: Int, scheme: HttpScheme): String {
	val trimmed = this.trim().trimEnd('/')
	require(trimmed.isNotBlank()) { "Not a valid url or a empty string." }
	val withScheme = "${scheme.value}://$trimmed:$port"
	val parsed = withScheme.toHttpUrlOrNull() ?: throw IllegalArgumentException("Enter a valid server address.")
	return parsed.toString().trimEnd('/')
}

enum class HttpScheme(val value: String) {
	Http("http"), Https("https");
	
	companion object {
		
		fun fromHttpUrl(url: HttpUrl): HttpScheme {
			return if (url.scheme == "http") Http else Https
		}
	}
}

fun buildUrl(base: String, path: String, parameters: Map<String, String?>): HttpUrl {
	val baseUrl = base.toUrl().toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid Emby server address: $base")
	return baseUrl.newBuilder().addPathSegments(path.trimStart('/')).apply {
		parameters.forEach { (name, value) -> value?.let { addQueryParameter(name, it) } }
	}.build()
}