package app.deference.embycl.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import app.deference.embycl.data.repository.update.fileMatchesDigest
import app.deference.embycl.domain.appupdate.UpdateInfo
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.platform.GithubUpdateProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

@Single
actual class AppUpdater(
    context: Context,
    @Named("download") private val client: HttpClient,
) {
    private val context = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile
    private var selected: UpdateInfo? = null
    private val directory get() = File(context.cacheDir, "updates").apply { check(mkdirs() || isDirectory) }
    private val apk get() = File(directory, "update.apk")
    private val pending get() = File(directory, "pending.json")

    actual suspend fun checkForUpdates(
        release: GithubRelease,
        userInitiated: Boolean,
    ): AppUpdate = withContext(Dispatchers.IO) {
        try {
            val info = GithubUpdateProvider.findUpdate(
                release = release,
                installedVersionCode = installedCode(),
            )
            selected = info
            when {
                info == null -> {
                    discardDownload()
                    AppUpdate.NotAvailable
                }
                restoreDownload(offered = info) -> AppUpdate.ReadyToInstall(release = info.release)
                else -> AppUpdate.Available(release = info.release)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            selected = null
            AppUpdate.Failed(
                message = e.message ?: "Could not check for updates. Check your connection and try again.",
                stage = UpdateStage.Check,
            )
        }
    }

    actual fun download(): Flow<AppUpdate> = channelFlow {
        val info = selected ?: run {
            send(AppUpdate.Failed(message = "Check for updates again before downloading.", stage = UpdateStage.Check))
            return@channelFlow
        }
        val partial = File(directory, "update.apk.part")
        try {
            send(AppUpdate.Downloading(release = info.release, downloaded = 0, total = info.size))
            partial.delete()
            client.prepareGet(urlString = info.url).execute { response ->
                check(response.status.isSuccess()) { "Could not download the update (HTTP ${response.status.value})." }
                val channel = response.bodyAsChannel()
                partial.outputStream().buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = channel.readAvailable(buffer)
                        if (count == -1) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        downloaded += count
                        check(downloaded <= info.size) { "The downloaded APK is larger than expected." }
                        val now = System.nanoTime()
                        if (now - lastReport >= PROGRESS_INTERVAL_NANOS || downloaded == info.size) {
                            send(AppUpdate.Downloading(release = info.release, downloaded = downloaded, total = info.size))
                            lastReport = now
                        }
                    }
                }
            }
            verifyFile(file = partial, info = info)
            verifyPackage(file = partial)
            check(!apk.exists() || apk.delete()) { "Could not replace the previous download." }
            check(partial.renameTo(apk)) { "Could not save the downloaded update." }
            pending.writeText(json.encodeToString(info))
            send(AppUpdate.ReadyToInstall(release = info.release))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            send(
                AppUpdate.Failed(
                    message = e.message ?: "Download failed. Check your connection and try again.",
                    stage = UpdateStage.Download,
                    release = info.release,
                ),
            )
        } finally {
            partial.delete()
        }
    }.flowOn(Dispatchers.IO)

    actual suspend fun install(): AppUpdate {
        val info = selected
            ?: return AppUpdate.Failed(message = "Check for updates again before installing.", stage = UpdateStage.Check)
        var stage = UpdateStage.Download
        return try {
            withContext(Dispatchers.IO) {
                verifyFile(file = apk, info = info)
                verifyPackage(file = apk)
            }
            stage = UpdateStage.Install
            if (!canInstallPackages()) {
                startActivity(
                    intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri()),
                )
                AppUpdate.AwaitingPermission(release = info.release)
            } else {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                startActivity(
                    intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
                // Launching an installer is not proof that installation succeeded.
                AppUpdate.InstallerLaunched(release = info.release)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppUpdate.Failed(
                message = e.message ?: "Could not open the installer.",
                stage = stage,
                release = info.release,
            )
        }
    }

    actual fun onAppResumed(current: AppUpdate): AppUpdate = when (current) {
        is AppUpdate.AwaitingPermission -> if (canInstallPackages()) AppUpdate.ReadyToInstall(release = current.release) else current
        is AppUpdate.InstallerLaunched -> AppUpdate.ReadyToInstall(release = current.release)
        AppUpdate.Idle,
        AppUpdate.Checking,
        AppUpdate.NotAvailable,
        is AppUpdate.Available,
        is AppUpdate.Downloading,
        is AppUpdate.ReadyToInstall,
        is AppUpdate.Failed -> current
    }

    private fun canInstallPackages(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    private suspend fun startActivity(intent: Intent) = withContext(Dispatchers.Main) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun installedCode(): Long = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    private fun restoreDownload(offered: UpdateInfo): Boolean {
        return !(!pending.isFile || !apk.isFile) && try {
            val saved = json.decodeFromString<UpdateInfo>(pending.readText())
            check(saved.release.versionCode == offered.release.versionCode) { "The saved download is for another release." }
            verifyFile(file = apk, info = saved)
            verifyPackage(file = apk)
            selected = saved
            true
        } catch (_: Exception) {
            discardDownload()
            false
        }
    }

    private fun discardDownload() {
        pending.delete()
        apk.delete()
    }

    private fun verifyFile(
        file: File,
        info: UpdateInfo,
    ) {
        check(file.isFile && file.length() == info.size) { "The APK is incomplete. Download the update again." }
        info.digest?.let { digest ->
            check(fileMatchesDigest(file = file, digest = digest)) { "The APK checksum does not match. Download the update again." }
        }
    }

    @Suppress("DEPRECATION")
    private fun verifyPackage(file: File) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("The downloaded file is not a valid APK.")
        val installed = pm.getPackageInfo(context.packageName, flags)
        val installedVersionCode = PackageInfoCompat.getLongVersionCode(installed)
        val candidateVersionCode = PackageInfoCompat.getLongVersionCode(candidate)
        check(candidateVersionCode > installedVersionCode) { "The update is not newer than the currently installed version." }
        check(candidate.packageName == context.packageName) { "The APK belongs to a different application." }
        check((candidate.applicationInfo?.minSdkVersion ?: 0) <= Build.VERSION.SDK_INT) { "This update requires a newer Android version." }
        val trusted = if (Build.VERSION.SDK_INT >= 28) {
            val current = installed.signingInfo ?: error("Could not read the installed app signature.")
            val incoming = candidate.signingInfo ?: error("The APK is not signed.")
            if (current.hasMultipleSigners() || incoming.hasMultipleSigners()) {
                current.apkContentsSigners.toSet() == incoming.apkContentsSigners.toSet()
            } else {
                incoming.signingCertificateHistory.toSet().containsAll(current.apkContentsSigners.toSet())
            }
        } else {
            installed.signatures?.toSet() == candidate.signatures?.toSet() && !candidate.signatures.isNullOrEmpty()
        }
        check(trusted) { "The APK was signed by a different publisher and cannot update this app." }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_INTERVAL_NANOS = 100_000_000L
    }
}
