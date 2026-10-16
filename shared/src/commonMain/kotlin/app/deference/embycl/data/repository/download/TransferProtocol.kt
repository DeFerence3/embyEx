package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.DownloadFailureReason
import io.ktor.http.*

internal class TransferStop(val failure: DownloadFailureReason? = null, val authentication: Boolean = false) : Exception()
internal class TransferRetry(
    val failure: DownloadFailureReason? = null,
    val restart: Boolean = false,
    val retryAfterMillis: Long = 0,
) : Exception()
internal class TransferStale : Exception()

internal object TransferProtocol {
    fun header(headers: Headers, name: String): String? {
        val values = headers.getAll(name) ?: return null
        if (values.size != 1) invalid()
        return values.single().trim()
    }
    fun length(headers: Headers): Long? = header(headers, HttpHeaders.ContentLength)?.let {
        positive(it) ?: invalid()
    }
    fun positive(value: String): Long? = value.takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull()?.takeIf { it > 0 }
    fun validateBody(headers: Headers) {
        val encoding = header(headers, HttpHeaders.ContentEncoding)
        if (encoding != null && !encoding.equals("identity", true)) invalid()
        if (headers[HttpHeaders.TransferEncoding] != null && headers[HttpHeaders.ContentLength] != null) invalid()
        val type = header(headers, HttpHeaders.ContentType)?.substringBefore(';')?.trim()?.lowercase()
        if (type?.startsWith("text/") == true || type?.startsWith("multipart/") == true ||
            type?.endsWith("+json") == true || type?.endsWith("+xml") == true || type in setOf(
                "application/json", "application/xml", "application/vnd.apple.mpegurl", "application/x-mpegurl", "application/dash+xml",
            )) invalid()
    }
    /** Initial implementation accepts the complete suffix requested by bytes=N-. */
    fun range(headers: Headers, offset: Long): Long {
        val match = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)", RegexOption.IGNORE_CASE)
            .matchEntire(header(headers, HttpHeaders.ContentRange).orEmpty()) ?: invalid()
        val start = match.groupValues[1].toLongOrNull() ?: invalid()
        val end = match.groupValues[2].toLongOrNull() ?: invalid()
        val total = positive(match.groupValues[3]) ?: invalid()
        if (start != offset || start > end || end != total - 1) invalid()
        if (length(headers)?.let { it != total - start } == true) invalid()
        return total
    }
    fun unsatisfiedRange(headers: Headers) {
        val match = Regex("bytes \\*/([0-9]+)", RegexOption.IGNORE_CASE)
            .matchEntire(header(headers, HttpHeaders.ContentRange).orEmpty()) ?: invalid()
        if (match.groupValues[1].toLongOrNull() == null) invalid()
    }
    fun retryAfter(headers: Headers, now: Long): Long {
        val value = headers.getAll(HttpHeaders.RetryAfter)?.singleOrNull()?.trim() ?: return 0
        if (value.matches(Regex("[0-9]+"))) {
            val seconds = value.toLongOrNull() ?: return Long.MAX_VALUE
            return if (seconds > Long.MAX_VALUE / 1000) Long.MAX_VALUE else seconds * 1000
        }
        return runCatching { (value.fromHttpToGmtDate().timestamp - now).coerceAtLeast(0) }.getOrDefault(0)
    }
    fun invalid(): Nothing = throw TransferStop(DownloadFailureReason.INVALID_RESPONSE)
}
