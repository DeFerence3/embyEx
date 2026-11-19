package app.deference.embycl.data.repository

import app.deference.embycl.core.utils.JsonUtils.json
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.CurrentBuildRelease
import app.deference.embycl.domain.model.update.ReleaseVersion
import app.deference.embycl.domain.repository.GithubRepo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class GithubRepoImpl(
    @Named("download") private val client: HttpClient,
) : GithubRepo {
    override suspend fun getLatestRelease(prerelease: Boolean): GithubRelease = if (prerelease) {
        json.decodeFromString<List<GithubRelease>>(string = getText(url = RELEASES_URL))
            .filterNot { it.draft }
            .mapNotNull { release -> ReleaseVersion.parse(text = release.tag)?.let { release to it } }
            .maxByOrNull { (_, version) -> version }
            ?.first
            ?: error("No published releases were found. Please try again later.")
    } else {
        json.decodeFromString<GithubRelease>(string = getText(url = LATEST_RELEASE_URL))
    }

    private suspend fun getText(url: String): String {
        val response = client.get(urlString = url) {
            header(
                key = "Accept",
                value = "application/vnd.github+json",
            )
            header(
                key = "User-Agent",
                value = "EmbyEx/${CurrentBuildRelease.tag}",
            )
            timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS }
        }
        check(response.status.isSuccess()) {
            if (response.status.value in listOf(HTTP_FORBIDDEN, HTTP_TOO_MANY_REQUESTS)) {
                "GitHub temporarily refused the update check. Try again later."
            } else {
                "Could not check for updates (HTTP ${response.status.value})."
            }
        }
        return response.bodyAsText()
    }

    private companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/DeFerence3/embyEx/releases/latest"
        const val RELEASES_URL = "https://api.github.com/repos/DeFerence3/embyEx/releases?per_page=30"
        const val REQUEST_TIMEOUT_MILLIS = 20_000L
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
