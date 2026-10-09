package app.deference.embycl

import app.deference.embycl.data.repository.ReleaseNotesRepository
import app.deference.embycl.domain.model.update.CurrentBuildRelease
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ReleaseNotesRepositoryTest {
    @Test fun fetchesInstalledTagAndRendersBody() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(CurrentBuildRelease.apiUrl, request.url.toString())
            assertTrue(request.url.encodedPath.endsWith("/tags/v${BuildConfig.VERSION_NAME.removePrefix("v")}"))
            assertNull(request.headers["X-Emby-Token"])
            assertNull(request.headers["Authorization"])
            respond("""{"body":"## Changes\nFixed playback.","extra":"ignored"}""")
        })
        try {
            assertEquals("## Changes\nFixed playback.", ReleaseNotesRepository(client).currentBuildChangelog())
        } finally { client.close() }
    }

    @Test fun missingReleaseDoesNotSubstituteLatestRelease() = runBlocking {
        var requests = 0
        val client = HttpClient(MockEngine { requests++; respond("{}", HttpStatusCode.NotFound) })
        try {
            val error = assertFailsWith<IllegalStateException> { ReleaseNotesRepository(client).currentBuildChangelog() }
            assertTrue(error.message.orEmpty().contains(CurrentBuildRelease.tag))
            assertEquals(1, requests)
        } finally { client.close() }
    }

    @Test fun absentBodyIsAnEmptyChangelog() = runBlocking {
        val client = HttpClient(MockEngine { respond("""{"body":null}""") })
        try { assertEquals("", ReleaseNotesRepository(client).currentBuildChangelog()) }
        finally { client.close() }
    }

    @Test fun rateLimitHasUsefulMessage() = runBlocking {
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.TooManyRequests) })
        try {
            val error = assertFailsWith<IllegalStateException> { ReleaseNotesRepository(client).currentBuildChangelog() }
            assertTrue(error.message.orEmpty().contains("try again later"))
        } finally { client.close() }
    }
}
