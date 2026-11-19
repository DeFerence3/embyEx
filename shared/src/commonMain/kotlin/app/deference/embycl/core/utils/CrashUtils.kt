package app.deference.embycl.core.utils

import app.deference.embycl.core.constants.GITHUB_NEW_ISSUE_URL
import app.deference.embycl.core.constants.MAX_ENCODED_STACK_TRACE_CHARS
import app.deference.embycl.core.constants.MAX_STACK_TRACE_CHARS
import app.deference.embycl.getPlatform
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodeURLQueryComponent

fun constructEnv(
	environment: List<Pair<String, String>>,
): String = buildString {
	environment.forEach { env ->
		appendLine(value = "- ${env.first}: ${env.second}")
	}
}.trimIndent()

fun buildCrashIssueUri(
	exceptionString: String,
	environment: List<Pair<String, String>>,
): Url {
	var stackTrace = exceptionString.take(n = MAX_STACK_TRACE_CHARS)
	// Bound the encoded size too: non-ASCII exception messages expand in a URL.
	while (stackTrace.encodeURLQueryComponent().length > MAX_ENCODED_STACK_TRACE_CHARS) {
		stackTrace = stackTrace.dropLast(100)
	}
	val platform = getPlatform()
	val crashLog = buildString {
		appendLine(value = stackTrace.ifBlank { "No exception details were supplied." })
		if (stackTrace.length < exceptionString.length) {
			appendLine(value = "… stack trace truncated. (Just paste full log, its in your clipboard).")
		}
	}
	return URLBuilder(GITHUB_NEW_ISSUE_URL).apply {
		parameters.append("query", "kotlin")
		parameters.append("sort", "recent")
		parameters.append("sort", "recent")
		parameters.append("template", "crash_report.yml")
		parameters.append("title", "[Crash] EmbyEx — ${platform.platform.name}")
		parameters.append("environment", constructEnv(environment = environment))
		parameters.append("crash_log", crashLog)
	}.build()
}
