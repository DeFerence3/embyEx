package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.download.DownloadConnection
import app.deference.embycl.domain.model.download.DownloadKey
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom

/** Runtime requests only. Persist DownloadKey, then rebuild against the current connection. */
internal object DownloadRequests {
    fun metadata(connection: DownloadConnection, vararg segments: String, authenticated: Boolean = true): HttpRequestBuilder =
        HttpRequestBuilder().apply {
            url.takeFrom(apiUrl(connection, *segments))
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, "EmbyEx")
            if (authenticated) header("X-Emby-Token", connection.accessToken)
        }

    fun media(connection: DownloadConnection, key: DownloadKey): HttpRequestBuilder {
        require(connection.serverId == key.serverId && connection.userId == key.userId) {
            "Download belongs to a different account."
        }
        return metadata(connection, "Items", key.itemId, "Download").apply {
            url.parameters.append("mediaSourceId", key.mediaSourceId)
            headers.remove(HttpHeaders.Accept)
            header(HttpHeaders.Accept, "*/*")
            // Ranges and byte counts must describe the original representation.
            header(HttpHeaders.AcceptEncoding, "identity")
        }
    }

    private fun apiUrl(connection: DownloadConnection, vararg segments: String): Url {
        val base = Url(connection.baseUrl)
        require(base.protocol == URLProtocol.HTTP || base.protocol == URLProtocol.HTTPS) {
            "Server address must use HTTP or HTTPS."
        }
        require(base.host.isNotBlank() && base.user.isNullOrEmpty() && base.password.isNullOrEmpty() &&
            base.parameters.isEmpty() && base.fragment.isEmpty() && !connection.baseUrl.contains('\\')) {
            "Server address must not contain credentials, query parameters, or a fragment."
        }
        require(segments.all { it.isNotBlank() && it != "." && it != ".." }) { "Invalid API path segment." }
        return URLBuilder(base).apply {
            encodedPath = encodedPath.trimEnd('/')
            appendPathSegments(*segments, encodeSlash = true)
        }.build()
    }
}
