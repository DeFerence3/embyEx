package app.deference.embycl

import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateRelease
import app.deference.embycl.platform.GithubUpdateProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GithubUpdateProviderTest {
    private fun client(
        versionCode: Int = 2,
        applicationId: String = "app.test",
        fileName: String = "app.apk",
        body: String = "\"notes\"",
        digest: String = "null",
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = HttpClient(MockEngine { request ->
        if (request.url.host == "api.github.com") respond(
            """{"body":$body,"assets":[
              {"name":"output-metadata.json","size":100,"browser_download_url":"https://example.test/metadata"},
              {"name":"$fileName","size":1234,"digest":$digest,"browser_download_url":"https://example.test/apk","content_type":"application/octet-stream"}
            ]}""", status
        ) else respond(
            """{"version":3,"applicationId":"$applicationId","variantName":"release","elements":[
              {"type":"SINGLE","versionCode":$versionCode,"versionName":"1.2.3","outputFile":"app.apk"}
            ]}"""
        )
    }) { install(HttpTimeout) }

    @Test fun newerCodeIsAvailableEvenWithSameDisplayVersionAndGenericMimeType() = runBlocking<Unit> {
        client().use { http ->
            val update = GithubUpdateProvider(http).findUpdate("app.test", 1)!!
            assertEquals(2L, update.release.versionCode)
            assertEquals("notes", update.release.changeLogMarkDown)
            assertNull(update.digest)
        }
    }

    @Test fun sameAndOlderCodesAreNotUpdates() = runBlocking<Unit> {
        for (remote in listOf(1, 2)) client(versionCode = remote).use {
            assertNull(GithubUpdateProvider(it).findUpdate("app.test", 2))
        }
    }

    @Test fun nullableNotesAndDigestAreAccepted() = runBlocking<Unit> {
        client(body = "null").use {
            assertEquals("", GithubUpdateProvider(it).findUpdate("app.test", 1)!!.release.changeLogMarkDown)
        }
    }

    @Test fun rejectsDifferentApplication() = runBlocking<Unit> {
        client(applicationId = "another.app").use {
            assertFailsWith<IllegalArgumentException> { GithubUpdateProvider(it).findUpdate("app.test", 1) }
        }
    }

    @Test fun requiresExactMetadataFilename() = runBlocking<Unit> {
        client(fileName = "different.apk").use {
            assertFailsWith<IllegalStateException> { GithubUpdateProvider(it).findUpdate("app.test", 1) }
        }
    }

    @Test fun checksHttpStatusBeforeDeserializing() = runBlocking<Unit> {
        val http = HttpClient(MockEngine { respond("not JSON", HttpStatusCode.Forbidden) }) { install(HttpTimeout) }
        http.use {
            val failure = assertFailsWith<IllegalStateException> { GithubUpdateProvider(it).findUpdate("app.test", 1) }
            assertTrue(failure.message!!.contains("GitHub"))
        }
    }

    @Test fun progressIsNormalizedAndUnknownLengthIsIndeterminate() {
        val release = UpdateRelease("test", 2, "")
        assertEquals(0.5f, AppUpdate.Downloading(release, 50, 100).fraction)
        assertEquals(1f, AppUpdate.Downloading(release, 200, 100).fraction)
        assertNull(AppUpdate.Downloading(release, 0, 0).fraction)
    }
}
