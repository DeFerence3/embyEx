package app.deference.embycl.core.utils

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments

fun <T> T?.or(predicate: () -> T): T = this ?: predicate()

fun <T> T?.or(predicate: () -> String): String = this?.toString() ?: predicate()

/**
 * converts any string into a url format (appends https and trims '/' or any characters)
 */
fun String.toUrl(): String {
	val parsed = this.toHttpUrl()
	return parsed.toString().trimEnd('/')
}

fun String.toUrl(port: Int, scheme: HttpScheme): String {
	val parsed = this.toHttpUrl(port,scheme)
	return parsed.toString().trimEnd('/')
}

fun String.toHttpUrl(): Url{
	val trimmed = this.trim().trimEnd('/')
	require(trimmed.isNotBlank()) { "Not a valid url or a empty string." }
	val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
	return runCatching { Url(withScheme) }.getOrNull() ?: throw IllegalArgumentException("Enter a valid server address.")
}

fun String.toHttpUrl(port: Int, scheme: HttpScheme): Url{
	val trimmed = this.trim().trimEnd('/')
	require(trimmed.isNotBlank()) { "Not a valid url or a empty string." }
	val withScheme = "${scheme.value}://$trimmed:$port"
	return runCatching { Url(withScheme) }.getOrNull() ?: throw IllegalArgumentException("Enter a valid server address.")
}

enum class HttpScheme(val value: String) {
	Http("http"), Https("https");
	
	companion object {
		
		fun fromHttpUrl(url: Url): HttpScheme {
			return if (url.protocol.name.lowercase() == "http") Http else Https
		}
	}
}

fun buildUrl(base: String, path: String, qParameters: Map<String, String?>): Url {
	val baseUrl = base.toUrl()
	return URLBuilder(baseUrl).apply{
		appendPathSegments(path.trimStart('/'))
		qParameters.forEach { (name, value) ->
			value?.let { parameters.append(name, it) }
		}
	}.build()
}