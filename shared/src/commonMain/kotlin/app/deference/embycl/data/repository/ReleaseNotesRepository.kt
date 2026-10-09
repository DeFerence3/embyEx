package app.deference.embycl.data.repository

import app.deference.embycl.core.utils.resolveLinks
import app.deference.embycl.domain.model.update.CurrentBuildRelease
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class ReleaseNotesRepository(@Named("download") private val client: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun currentBuildChangelog(): String {
        // The download client has no Emby authentication or server URL interception.
        val response = client.get(CurrentBuildRelease.apiUrl) {
            header("Accept", "application/vnd.github+json")
            header("User-Agent", "EmbyEx/${CurrentBuildRelease.tag}")
            timeout { requestTimeoutMillis = 20_000 }
        }
        check(response.status.value in 200..299) {
            when (response.status.value) {
                404 -> "Release notes for ${CurrentBuildRelease.tag} haven’t been published yet."
                403, 429 -> "GitHub temporarily limited requests. Please try again later."
                else -> "Could not load release notes (HTTP ${response.status.value})."
            }
        }
        return json.decodeFromString<ReleaseNotes>(response.bodyAsText()).body.orEmpty().resolveLinks()
    }
}

@Serializable
private data class ReleaseNotes(val body: String? = null)
