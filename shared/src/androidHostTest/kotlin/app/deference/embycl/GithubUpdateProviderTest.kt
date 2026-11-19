package app.deference.embycl

import app.deference.embycl.data.repository.GithubRepoImpl
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
    // v1.2.3 has version code 1_020_300.
    private fun client(
        tag: String = "v1.2.3",
        fileName: String = "app.apk",
        body: String = "\"notes\"",
        digest: String = "null",
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = HttpClient(MockEngine {
        respond(
            """{"tag_name":"$tag","body":$body,"assets":[
              {"name":"$fileName","size":1234,"digest":$digest,"browser_download_url":"https://example.test/apk","content_type":"application/octet-stream"}
            ]}""", status
        )
    }) { install(HttpTimeout) }

    private suspend fun findUpdate(
        http: HttpClient,
        installedVersionCode: Long,
    ) = GithubUpdateProvider.findUpdate(
        release = GithubRepoImpl(client = http).getLatestRelease(),
        installedVersionCode = installedVersionCode,
    )

    @Test fun newerTagIsAvailableWithItsVersionCodeAndGenericMimeType() = runBlocking<Unit> {
        client().use { http ->
            val update = findUpdate(http, 1_020_299)!!
            assertEquals(1_020_300L, update.release.versionCode)
            assertEquals("1.2.3", update.release.version)
            assertEquals("notes", update.release.changeLogMarkDown)
            assertNull(update.digest)
        }
    }

    @Test fun betaTagSitsBetweenReleases() = runBlocking<Unit> {
        client(tag = "v1.2.4-beta.2").use {
            assertEquals(1_020_302L, findUpdate(it, 1_020_300)!!.release.versionCode)
        }
    }

    @Test fun sameAndOlderTagsAreNotUpdates() = runBlocking<Unit> {
        for (installed in listOf(1_020_300L, 1_020_301L)) client().use {
            assertNull(findUpdate(it, installed))
        }
    }

    @Test fun nullableNotesAndDigestAreAccepted() = runBlocking<Unit> {
        client(body = "null").use {
            assertEquals("", findUpdate(it, 1)!!.release.changeLogMarkDown)
        }
    }

    @Test fun rejectsTagThatIsNotAVersion() = runBlocking<Unit> {
        client(tag = "nightly").use {
            assertFailsWith<IllegalStateException> { findUpdate(it, 1) }
        }
    }

    @Test fun requiresAnApkAsset() = runBlocking<Unit> {
        client(fileName = "app.msi").use {
            assertFailsWith<IllegalStateException> { findUpdate(it, 1) }
        }
    }

    @Test fun checksHttpStatusBeforeDeserializing() = runBlocking<Unit> {
        val http = HttpClient(MockEngine { respond("not JSON", HttpStatusCode.Forbidden) }) { install(HttpTimeout) }
        http.use {
            val failure = assertFailsWith<IllegalStateException> { findUpdate(it, 1) }
            assertTrue(failure.message!!.contains("GitHub"))
        }
    }

    @Test fun progressIsNormalizedAndUnknownLengthIsIndeterminate() {
        val release = UpdateRelease(version = "test", tag = "test", versionCode = 2L, changeLogMarkDown = "")
        assertEquals(0.5f, AppUpdate.Downloading(release, 50, 100).fraction)
        assertEquals(1f, AppUpdate.Downloading(release, 200, 100).fraction)
        assertNull(AppUpdate.Downloading(release, 0, 0).fraction)
    }
}
