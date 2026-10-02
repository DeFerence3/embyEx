package app.deference.embycl.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import app.deference.embycl.domain.appupdate.UpdateInfo
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.domain.model.update.UpdateState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/** Application-owned work survives navigation. One mutex serializes checks, downloads and install requests. */
class AndroidUpdateController(
    private val context: Context,
    private val client: HttpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 30_000
            // Downloads may legitimately take minutes; only stalled connections time out.
        }
    },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val _state = MutableStateFlow(UpdateState())
    val state = _state.asStateFlow()
    private val operation = Mutex()
    private val provider = GithubUpdateProvider(client)
    private var selected: UpdateInfo? = null
    private var checkedAutomatically = false
    private val dismissVersion = AtomicLong()
    private val directory get() = File(context.cacheDir, "updates").apply { check(mkdirs() || isDirectory) }
    private val apk get() = File(directory, "update.apk")
    private val pending get() = File(directory, "pending.json")

    fun dismiss() {
        dismissVersion.incrementAndGet()
        _state.update { it.copy(dialogVisible = false) }
    }

    fun checkForUpdates(userInitiated: Boolean) {
        if (userInitiated) _state.update { it.copy(dialogVisible = true) }
        if (!operation.tryLock()) return
        val dismissalAtStart = dismissVersion.get()
        scope.launch {
            try {
                if (!userInitiated && checkedAutomatically) return@launch
                checkedAutomatically = true
                // Reopening from Settings shows the existing operation or verified download.
                if (selected != null && state.value.update !is AppUpdate.Failed) return@launch
                if (restoreDownload()) {
                    publish(AppUpdate.ReadyToInstall(selected!!.release), reveal = !userInitiated && dismissVersion.get() == dismissalAtStart)
                    return@launch
                }
                selected = null
                publish(AppUpdate.Checking)
                val info = provider.findUpdate(context.packageName, installedCode())
                selected = info
				val update = info?.let { AppUpdate.Available(it.release) } ?: AppUpdate.NotAvailable
				val reveal = info != null && !userInitiated && dismissVersion.get() == dismissalAtStart
                publish(update,reveal)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish(AppUpdate.Failed(e.message ?: "Could not check for updates. Check your connection and try again.", UpdateStage.Check))
            } finally {
                operation.unlock()
            }
        }
    }

    fun download() {
        if (!operation.tryLock()) return
        scope.launch {
            var partial: File? = null
            try {
                val info = selected ?: return@launch
                if (state.value.update !is AppUpdate.Available && state.value.update !is AppUpdate.Failed) return@launch
                publish(AppUpdate.Downloading(info.release, 0, info.size))
                partial = File(directory, "update.apk.part")
                partial.delete()
                client.prepareGet(info.url).execute { response ->
                    check(response.status.isSuccess()) { "Could not download the update (HTTP ${response.status.value})." }
                    val channel = response.bodyAsChannel()
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastReport = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = channel.readAvailable(buffer)
                            if (count == -1) break
                            if (count == 0) continue
                            output.write(buffer, 0, count)
                            downloaded += count
                            check(downloaded <= info.size) { "The downloaded APK is larger than expected." }
                            val now = System.nanoTime()
                            if (now - lastReport >= 100_000_000 || downloaded == info.size) {
                                publish(AppUpdate.Downloading(info.release, downloaded, info.size))
                                lastReport = now
                            }
                        }
                    }
                }
                verifyFile(partial, info)
                verifyPackage(partial, info)
                check(!apk.exists() || apk.delete()) { "Could not replace the previous download." }
                check(partial.renameTo(apk)) { "Could not save the downloaded update." }
                pending.writeText(updateJson.encodeToString(info))
                publish(AppUpdate.ReadyToInstall(info.release))
            } catch (e: CancellationException) {
                selected?.let { publish(AppUpdate.Available(it.release)) }
                throw e
            } catch (e: Exception) {
                publish(AppUpdate.Failed(e.message ?: "Download failed. Check your connection and try again.", UpdateStage.Download, selected?.release))
            } finally {
                partial?.delete()
                operation.unlock()
            }
        }
    }

    fun install() {
        if (!operation.tryLock()) return
        scope.launch {
            var stage = UpdateStage.Download
            try {
                val info = selected ?: return@launch
                when (state.value.update) {
                    is AppUpdate.ReadyToInstall, is AppUpdate.AwaitingPermission -> Unit
                    is AppUpdate.Failed -> if ((state.value.update as AppUpdate.Failed).stage != UpdateStage.Install) return@launch
                    else -> return@launch
                }
                verifyFile(apk, info)
                verifyPackage(apk, info)
                stage = UpdateStage.Install
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    publish(AppUpdate.AwaitingPermission(info.release))
                    withContext(Dispatchers.Main) {
                        context
							.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    return@launch
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                withContext(Dispatchers.Main) {
                    context
						.startActivity(Intent(Intent.ACTION_VIEW)
						.setDataAndType(uri, "application/vnd.android.package-archive")
						.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                // Launching an installer is not proof that installation succeeded.
                publish(AppUpdate.InstallerLaunched(info.release))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish(AppUpdate.Failed(e.message ?: "Could not open the installer.", stage, selected?.release))
            } finally {
                operation.unlock()
            }
        }
    }

    fun onAppResumed() {
        if (!operation.tryLock()) return
        scope.launch {
            try {
                val info = selected ?: return@launch
                when (state.value.update) {
                    is AppUpdate.AwaitingPermission -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                            publish(AppUpdate.ReadyToInstall(info.release))
                        }
                    }
                    is AppUpdate.InstallerLaunched -> publish(AppUpdate.ReadyToInstall(info.release))
                    else -> Unit
                }
            } finally {
                operation.unlock()
            }
        }
    }

    private fun publish(update: AppUpdate, reveal: Boolean = false) =
        _state.update { it.copy(update = update, dialogVisible = it.dialogVisible || reveal) }

    private fun installedCode(): Long = PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    private fun restoreDownload(): Boolean {
        if (!pending.isFile || !apk.isFile) return false
        return try {
            val info = updateJson.decodeFromString<UpdateInfo>(pending.readText())
            verifyFile(apk, info)
            verifyPackage(apk, info)
            selected = info
            true
        } catch (_: Exception) {
            pending.delete()
            apk.delete()
            false
        }
    }

    private fun verifyFile(file: File, info: UpdateInfo) {
        check(file.isFile && file.length() == info.size) { "The APK is incomplete. Download the update again." }
        info.digest?.let { digest ->
            val parts = digest.split(':', limit = 2)
            val algorithm = when (parts.firstOrNull()?.lowercase()) {
                "sha256" -> "SHA-256"
                "sha512" -> "SHA-512"
                else -> error("The release uses an unsupported checksum.")
            }
            check(parts.size == 2) { "The release checksum is invalid." }
            val hash = MessageDigest.getInstance(algorithm)
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    hash.update(buffer, 0, count)
                }
            }
            val actual = hash.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(actual.equals(parts[1], ignoreCase = true)) { "The APK checksum does not match. Download the update again." }
        }
    }

    private fun verifyPackage(file: File, info: UpdateInfo) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidate = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("The downloaded file is not a valid APK.")
        val installed = pm.getPackageInfo(context.packageName, flags)
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
}
