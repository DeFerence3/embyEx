package app.deference.embycl.core.utils

import app.deference.embycl.Platforms.Android
import app.deference.embycl.Platforms.IOS
import app.deference.embycl.Platforms.JVM
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
	val report = buildString {
		appendLine(value = "## Summary")
		appendLine(value = "EmbyEx crashed unexpectedly. Please help investigate this failure.")
		appendLine()
		appendLine(value = "## Environment")
		environment.forEach { env ->
			appendLine(value = "- ${env.first}: ${env.second}")
		}
		appendLine()
		appendLine(value = "## Crash log")
		appendLine(value = "```text")
		appendLine(value = stackTrace.ifBlank { "No exception details were supplied." })
		if (stackTrace.length < exceptionString.length) {
			appendLine(value = "… stack trace truncated. (Just paste full log, its in your clipboard).")
		}
		appendLine(value = "```")
		appendLine()
		appendLine(value = "## Expected behavior")
		appendLine(value = "(Please describe what you expected to happen.)")
		appendLine()
		appendLine(value = "## Steps to reproduce")
		appendLine(value = "1. (Step 1)")
		appendLine()
		val platformInfo = when(platform.platform){
			JVM -> "modified your system"
			Android -> "are using a custom rom"
			IOS -> "modified your system"
		}
		appendLine(value = "## Additional info")
		appendLine(value = "(like if you $platformInfo)")
		appendLine()
		appendLine(value = "_Generated locally by EmbyEx. Review this report and remove private information before submitting._")
	}
	return URLBuilder(GITHUB_NEW_ISSUE_URL).apply {
		parameters.append("query", "kotlin")
		parameters.append("sort", "recent")
		parameters.append("sort", "recent")
		parameters.append("template", "crash_report.md")
		parameters.append("title", "[Crash] EmbyEx — ${platform.platform.name}")
		parameters.append("body", report)
	}.build()
}
