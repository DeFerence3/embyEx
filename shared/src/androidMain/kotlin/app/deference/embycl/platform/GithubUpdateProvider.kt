package app.deference.embycl.platform

import app.deference.embycl.core.utils.normalizeReleaseBody
import app.deference.embycl.core.utils.resolveLinks
import app.deference.embycl.domain.appupdate.OutputMetadata
import app.deference.embycl.domain.appupdate.UpdateInfo
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.UpdateRelease
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

internal val updateJson = Json { ignoreUnknownKeys = true }

/** Notes, metadata and APK are resolved from the same release snapshot. */
class GithubUpdateProvider(
	private val client: HttpClient
) {
	
    suspend fun findUpdate(applicationId: String, installedVersionCode: Long): UpdateInfo? {
        val release = updateJson.decodeFromString<GithubRelease>(getText(RELEASE_URL))
		
        val metadataAsset = release.assets.singleOrNull { it.name == "output-metadata.json" } ?: error("This release is missing Android update metadata. Please try again later.")
		
        val metadata = updateJson.decodeFromString<OutputMetadata>(getText(metadataAsset.browserDownloadUrl))
		
        require(metadata.applicationId == applicationId) { "The update belongs to a different application." }
		
        // The release pipeline publishes one universal APK, not split APKs.
        val element = metadata.elements.singleOrNull { it.type == "SINGLE" } ?: error("This release does not contain a supported universal APK.")
		
        return if (element.versionCode.toLong() <= installedVersionCode) {
			null
		} else {
			val asset = release.assets.singleOrNull { it.name == element.outputFile && it.name.endsWith(".apk", true) } ?: error("The APK listed in the update metadata is missing.")
			require(asset.size > 0) { "The update APK is empty." }
			requireHttps(asset.browserDownloadUrl)
			val updateRelease = UpdateRelease(element.versionName, release.tag,element.versionCode.toLong(), release.body.orEmpty().normalizeReleaseBody().resolveLinks())
			UpdateInfo(
				updateRelease,
				asset.browserDownloadUrl, asset.digest, asset.size,
			)
		}
    }

    private suspend fun getText(url: String): String {
        requireHttps(url)
        val response = client.get(url) {
            header("Accept", "application/vnd.github+json")
            timeout { requestTimeoutMillis = 20_000 }
        }
        check(response.status.isSuccess()) {
            if (response.status.value in listOf(403, 429)) "GitHub temporarily refused the update check. Try again later."
            else "Could not check for updates (HTTP ${response.status.value})."
        }
        return response.bodyAsText()
    }

    private fun requireHttps(url: String) = require(url.startsWith("https://")) { "Update URLs must use HTTPS." }
	
    private companion object {
        const val RELEASE_URL = "https://api.github.com/repos/DeFerence3/embyEx/releases/latest"
    }
}
