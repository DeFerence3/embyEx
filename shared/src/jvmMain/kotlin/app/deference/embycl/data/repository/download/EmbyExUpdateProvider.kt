package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.ReleaseVersion
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.updater.provider.UpdateProvider
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Finds desktop updates in the GitHub releases of [owner]/[repo], reading the per-platform
 * manifests the release pipeline publishes (`latest-windows.yml`, `latest-linux.yml`).
 *
 * Without the beta opt-in, the manifest is read from GitHub's latest release, which never is a
 * pre-release. With it ([includePrerelease], read at every check), the REST API lists the recent
 * releases and the highest version wins, pre-release or not, so a beta tester moves on to the
 * release that follows their beta. Every file is downloaded from the release tagged `v<version>`.
 */
class EmbyExUpdateProvider(
    private val owner: String,
    private val repo: String,
    private val includePrerelease: () -> Boolean,
) : UpdateProvider {
    private val releasesUrl = "https://github.com/$owner/$repo/releases"
    private val releasesApiUrl = "https://api.github.com/repos/$owner/$repo/releases?per_page=$RELEASES_PER_PAGE"
    private val json = Json { ignoreUnknownKeys = true }

    override fun getUpdateMetadataUrl(
        channel: String,
        platform: Platform,
    ): String = "$releasesUrl/latest/download/${metadataFileName(platform = platform)}"

    /** Nucleus' own [channel] and pre-release rules are ignored: the beta opt-in decides. */
    override fun resolveMetadataUrl(
        channel: String,
        platform: Platform,
        httpClient: HttpClient,
        currentVersion: String,
        allowPrerelease: Boolean,
    ): String =
        if (includePrerelease()) {
            "$releasesUrl/download/${newestTag(httpClient = httpClient)}/${metadataFileName(platform = platform)}"
        } else {
            getUpdateMetadataUrl(
                channel = channel,
                platform = platform,
            )
        }

    override fun getDownloadUrl(
        fileName: String,
        version: String,
    ): String = "$releasesUrl/download/v$version/${fileName.substringAfterLast('/')}"

    /** The tag of the highest-versioned published release, pre-releases included. */
    private fun newestTag(httpClient: HttpClient): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(releasesApiUrl))
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == HTTP_OK) {
            if (response.statusCode() in listOf(HTTP_FORBIDDEN, HTTP_TOO_MANY_REQUESTS)) {
                "GitHub temporarily refused the update check. Try again later."
            } else {
                "Could not check for updates (HTTP ${response.statusCode()})."
            }
        }
        return json.decodeFromString<List<GithubRelease>>(string = response.body())
            .filterNot { it.draft }
            .mapNotNull { release -> ReleaseVersion.parse(text = release.tag)?.let { release.tag to it } }
            .maxByOrNull { (_, version) -> version }
            ?.first
            ?: throw NoSuchElementException("No published releases for $owner/$repo on GitHub.")
    }

    private fun metadataFileName(platform: Platform): String = when (platform) {
        Platform.Windows -> "latest-windows.yml"
        Platform.Linux -> "latest-linux.yml"
        Platform.MacOS,
        Platform.Unknown,
        -> throw UnsupportedOperationException("Updates are not published for $platform.")
    }

    private companion object {
        const val RELEASES_PER_PAGE = 30
        const val HTTP_OK = 200
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
