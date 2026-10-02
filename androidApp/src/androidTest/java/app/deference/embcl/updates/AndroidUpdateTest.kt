package app.deference.embcl

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.content.pm.PackageInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateRelease
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.platform.AndroidUpdateController
import app.deference.embycl.platform.GithubUpdateProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AndroidUpdateTest {
    private val target = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDir = File(target.cacheDir, "update-test-${UUID.randomUUID()}").apply { mkdirs() }
    private val context = object : ContextWrapper(target) {
        override fun getCacheDir() = testDir
        override fun startActivity(intent: Intent) { launchedIntent = intent }
    }
    private var launchedIntent: Intent? = null
    private val code = PackageInfoCompat.getLongVersionCode(target.packageManager.getPackageInfo(target.packageName, 0))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clients = mutableListOf<HttpClient>()
    private val requests = AtomicInteger()

    @After fun cleanup() {
        scope.cancel()
        clients.forEach { it.close() }
        testDir.deleteRecursively()
    }

    private fun client(
        bytes: ByteArray = "not an apk".toByteArray(),
        remoteCode: Long = code + 1,
        applicationId: String = target.packageName,
        assetName: String = "update.apk",
        checksum: String = sha256(bytes),
        status: HttpStatusCode = HttpStatusCode.OK,
        gate: CompletableDeferred<Unit>? = null,
    ): HttpClient = HttpClient(MockEngine { request ->
        requests.incrementAndGet()
        if (request.url.host == "api.github.com") {
            gate?.await()
            respond("""{"body":"Release notes","assets":[
                {"name":"output-metadata.json","size":100,"browser_download_url":"https://example.test/metadata"},
                {"name":"$assetName","size":${bytes.size},"digest":"sha256:$checksum","browser_download_url":"https://example.test/apk"}
            ]}""", status, headersOf("Content-Type", "application/json"))
        } else if (request.url.encodedPath == "/metadata") {
            respond("""{"version":3,"applicationId":"$applicationId","variantName":"release","elements":[
                {"type":"SINGLE","versionCode":$remoteCode,"versionName":"test-next","outputFile":"update.apk"}
            ]}""")
        } else respond(bytes)
    }) { install(HttpTimeout) }.also { clients += it }

    private suspend fun AndroidUpdateController.awaitState(predicate: (AppUpdate) -> Boolean): AppUpdate =
        withTimeout(10_000) { state.first { predicate(it.update) }.update }.also { delay(50) }

    @Test fun progressUsesFractionAndHandlesUnknownLength() {
        val release = UpdateRelease("test", 2, "")
        assertEquals(0.5f, AppUpdate.Downloading(release, 50, 100).fraction!!, 0.001f)
        assertEquals(1f, AppUpdate.Downloading(release, 110, 100).fraction!!, 0.001f)
        assertNull(AppUpdate.Downloading(release, 0, 0).fraction)
    }

    @Test fun installedVersionCodePreventsRepeatAndDowngradeOffers() = runBlocking<Unit> {
        assertNull(GithubUpdateProvider(client(remoteCode = code)).findUpdate(target.packageName, code))
        assertNull(GithubUpdateProvider(client(remoteCode = code - 1)).findUpdate(target.packageName, code))
    }

    @Test fun releaseNotesAndAssetComeFromSameSnapshot() = runBlocking<Unit> {
        val info = GithubUpdateProvider(client()).findUpdate(target.packageName, code)!!
        assertEquals(code + 1, info.release.versionCode)
        assertEquals("Release notes", info.release.changeLogMarkDown)
        assertEquals("https://example.test/apk", info.url)
        assertEquals(2, requests.get()) // no duplicate changelog request
    }

    @Test fun wrongApplicationAndMissingApkAreReported() = runBlocking<Unit> {
        for (http in listOf(client(applicationId = "wrong.package"), client(assetName = "other.apk"))) {
            val updater = AndroidUpdateController(context, http, scope)
            updater.checkForUpdates(true)
            val failure = updater.awaitState { it is AppUpdate.Failed } as AppUpdate.Failed
            assertEquals(UpdateStage.Check, failure.stage)
            assertTrue(updater.state.value.dialogVisible)
        }
    }

    @Test fun httpErrorBecomesRetryableState() = runBlocking<Unit> {
        val updater = AndroidUpdateController(context, client(status = HttpStatusCode.Forbidden), scope)
        updater.checkForUpdates(true)
        val failure = updater.awaitState { it is AppUpdate.Failed } as AppUpdate.Failed
        assertEquals(UpdateStage.Check, failure.stage)
        assertTrue(failure.message.contains("GitHub"))
    }

    @Test fun checksAreSerializedAndDismissalSurvivesCompletion() = runBlocking<Unit> {
        val gate = CompletableDeferred<Unit>()
        val updater = AndroidUpdateController(context, client(gate = gate), scope)
        updater.checkForUpdates(true)
        updater.awaitState { it == AppUpdate.Checking }
        repeat(20) { updater.checkForUpdates(true) }
        updater.dismiss()
        gate.complete(Unit)
        updater.awaitState { it is AppUpdate.Available }
        assertEquals(2, requests.get())
        assertFalse(updater.state.value.dialogVisible)
    }

    @Test fun checksumFailureNeverLaunchesInstallerAndRetryDownloadsOnce() = runBlocking<Unit> {
        val updater = AndroidUpdateController(context, client(checksum = "0".repeat(64)), scope)
        updater.checkForUpdates(true)
        updater.awaitState { it is AppUpdate.Available }
        repeat(20) { updater.download() }
        val failure = updater.awaitState { it is AppUpdate.Failed } as AppUpdate.Failed
        assertEquals(UpdateStage.Download, failure.stage)
        assertTrue(failure.message.contains("checksum"))
        assertEquals(3, requests.get())
        assertNull(launchedIntent)
        assertFalse(File(testDir, "updates/update.apk.part").exists())
        updater.download()
        updater.awaitState { it is AppUpdate.Failed }
        assertEquals(4, requests.get())
    }

    @Test fun nonApkPayloadIsRejectedEvenWithMatchingChecksum() = runBlocking<Unit> {
        val updater = AndroidUpdateController(context, client(), scope)
        updater.checkForUpdates(true)
        updater.awaitState { it is AppUpdate.Available }
        updater.download()
        val failure = updater.awaitState { it is AppUpdate.Failed } as AppUpdate.Failed
        assertTrue(failure.message.contains("valid APK"))
        assertNull(launchedIntent)
    }

    /** Optional real signed APK fixture: pass updateApk with an APK whose versionCode is installed + 1. */
    @Test fun verifiedDownloadIsRestoredWithoutNetwork() = runBlocking<Unit> {
        val path = InstrumentationRegistry.getArguments().getString("updateApk")
        assumeTrue("Supply a newer signed APK to exercise package validation and cache recovery", path != null)
        val bytes = File(path!!).readBytes()
        val http = client(bytes = bytes)
        val updater = AndroidUpdateController(context, http, scope)
        updater.checkForUpdates(true)
        updater.awaitState { it is AppUpdate.Available }
        updater.download()
        val completed = updater.awaitState { it is AppUpdate.ReadyToInstall || it is AppUpdate.Failed }
        assertTrue("Download failed: $completed", completed is AppUpdate.ReadyToInstall)
        val restored = AndroidUpdateController(context, http, scope)
        restored.checkForUpdates(true)
        restored.awaitState { it is AppUpdate.ReadyToInstall }
        assertEquals(3, requests.get())
        assertNull(launchedIntent)
    }

    @Test fun upToDateDialogIsOnlyShownForManualChecks() = runBlocking<Unit> {
        val updater = AndroidUpdateController(context, client(remoteCode = code), scope)
        updater.checkForUpdates(false)
        updater.awaitState { it == AppUpdate.NotAvailable }
        assertFalse(updater.state.value.dialogVisible)

        updater.checkForUpdates(true)
        updater.awaitState { it == AppUpdate.NotAvailable }
        assertTrue(updater.state.value.dialogVisible)
    }

    @Test fun manualCheckJoiningAutomaticCheckCanShowUpToDate() = runBlocking<Unit> {
        val gate = CompletableDeferred<Unit>()
        val updater = AndroidUpdateController(context, client(remoteCode = code, gate = gate), scope)
        updater.checkForUpdates(false)
        updater.awaitState { it == AppUpdate.Checking }
        assertFalse(updater.state.value.dialogVisible)
        updater.checkForUpdates(true)
        gate.complete(Unit)
        updater.awaitState { it == AppUpdate.NotAvailable }
        assertTrue(updater.state.value.dialogVisible)
        assertEquals(2, requests.get())
    }

    @Test fun dismissedManualCheckDoesNotShowUpToDateOnCompletion() = runBlocking<Unit> {
        val gate = CompletableDeferred<Unit>()
        val updater = AndroidUpdateController(context, client(remoteCode = code, gate = gate), scope)
        updater.checkForUpdates(true)
        updater.awaitState { it == AppUpdate.Checking }
        updater.dismiss()
        gate.complete(Unit)
        updater.awaitState { it == AppUpdate.NotAvailable }
        assertFalse(updater.state.value.dialogVisible)
    }

    @Test fun automaticCheckStillShowsAnAvailableUpdate() = runBlocking<Unit> {
        val updater = AndroidUpdateController(context, client(), scope)
        updater.checkForUpdates(false)
        updater.awaitState { it is AppUpdate.Available }
        assertTrue(updater.state.value.dialogVisible)
    }

    @Test fun automaticCheckDoesNotReopenDialogDismissedDuringCheck() = runBlocking<Unit> {
        val gate = CompletableDeferred<Unit>()
        val updater = AndroidUpdateController(context, client(gate = gate), scope)
        updater.checkForUpdates(false)
        updater.awaitState { it == AppUpdate.Checking }
        updater.checkForUpdates(true)
        updater.dismiss()
        gate.complete(Unit)
        updater.awaitState { it is AppUpdate.Available }
        assertFalse(updater.state.value.dialogVisible)
    }

    /** Opt-in interactive smoke test. Uses the actual download/validation path and Android installer. */
    @Test fun openRealInstallerOnDevice() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("exerciseInstaller") == "true")
        val bytes = File(requireNotNull(args.getString("updateApk"))).readBytes()
        val updater = AndroidUpdateController(target, client(bytes = bytes), scope)
        ActivityScenario.launch(MainActivity::class.java).use {
            updater.checkForUpdates(true)
            updater.awaitState { it is AppUpdate.Available }
            updater.download()
            val result = updater.awaitState { it is AppUpdate.ReadyToInstall || it is AppUpdate.Failed }
            assertTrue("Download failed: $result", result is AppUpdate.ReadyToInstall)
            updater.install()
            val handoff = updater.awaitState { it is AppUpdate.AwaitingPermission || it is AppUpdate.InstallerLaunched || it is AppUpdate.Failed }
            assertFalse("Installer handoff failed: $handoff", handoff is AppUpdate.Failed)
        }
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
