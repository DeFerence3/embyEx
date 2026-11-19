package app.deference.embycl.data.repository

import app.deference.embycl.BuildConfig
import app.deference.embycl.core.utils.normalizeReleaseBody
import app.deference.embycl.core.utils.resolveLinks
import app.deference.embycl.data.repository.download.PacmanUpdateInstaller
import app.deference.embycl.data.repository.download.UpdateAssetDownloader
import app.deference.embycl.domain.ghrelease.Asset
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.ReleaseVersion
import app.deference.embycl.domain.model.update.UpdateRelease
import app.deference.embycl.domain.model.update.UpdateStage
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.provider.GitHubProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

@Single
actual class AppUpdater(
	@Named("download")
	client: HttpClient
) {
	private val downloader = UpdateAssetDownloader(client = client)
	private val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
	private val pacmanInstaller by lazy { if (isWindows) null else PacmanUpdateInstaller.forCurrentProcess() }

	@Volatile
	private var asset: Asset? = null
	@Volatile
	private var updateRelease: UpdateRelease? = null
	@Volatile
	private var installer: File? = null
	private val updater = NucleusUpdater {
		provider = GitHubProvider(owner = "DeFerence3", repo = "embyEx")
		currentVersion = BuildConfig.VERSION_NAME
		executableType = if (isWindows) "msi" else "deb"
	}

	actual suspend fun checkForUpdates(
		release: GithubRelease,
		userInitiated: Boolean,
	): AppUpdate = withContext(Dispatchers.IO) {
		try {
			selectUpdate(release = release)
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			AppUpdate.Failed(
				message = e.message ?: "Could not check for updates. Check your connection and try again.",
				stage = UpdateStage.Check,
			)
		}
	}

	actual fun download(): Flow<AppUpdate> = channelFlow {
		val selected = asset
		val release = updateRelease
		if (selected == null || release == null) {
			send(AppUpdate.Failed(message = "Check for updates again before downloading.", stage = UpdateStage.Check))
			return@channelFlow
		}
		send(AppUpdate.Downloading(release = release, downloaded = 0, total = selected.size))
		try {
			installer = downloader.download(asset = selected) { downloaded ->
				trySend(AppUpdate.Downloading(release = release, downloaded = downloaded, total = selected.size))
			}
			send(AppUpdate.ReadyToInstall(release = release))
		} catch (e: CancellationException) {
			throw e
		} catch (e: Exception) {
			send(
				AppUpdate.Failed(
					message = e.message ?: "Could not download update. Check your connection and try again.",
					stage = UpdateStage.Download,
					release = release,
				),
			)
		}
	}

	actual suspend fun install(): AppUpdate {
		val release = updateRelease
			?: return AppUpdate.Failed(message = "Check for updates again before installing.", stage = UpdateStage.Check)
		val file = installer?.takeIf { it.isFile }
			?: return AppUpdate.Failed(
				message = "The download is missing. Download the update again.",
				stage = UpdateStage.Download,
				release = release,
			)
		return withContext(Dispatchers.IO) {
			try {
				val pacman = pacmanInstaller
				if (pacman == null) {
					updater.installAndRestart(file)
				} else {
					// Blocks while polkit asks for the password; a failure leaves the app running.
					pacman.install(packageFile = file)
					pacman.restart()
				}
				AppUpdate.InstallerLaunched(release = release)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				AppUpdate.Failed(
					message = e.message ?: "Could not install the update.",
					stage = UpdateStage.Install,
					release = release,
				)
			}
		}
	}

	actual fun onAppResumed(current: AppUpdate): AppUpdate = current

	private fun selectUpdate(release: GithubRelease): AppUpdate {
		val remote = ReleaseVersion.parse(text = release.tag) ?: error("The release ${release.tag} is not tagged with an app version.")
		val current = ReleaseVersion.parse(text = BuildConfig.VERSION_NAME) ?: error("This build has no release version, so it cannot be updated.")
		// A download of an earlier check is never installed for a different release.
		installer?.parentFile?.deleteRecursively()
		installer = null
		asset = null
		updateRelease = null
		if (remote <= current) return AppUpdate.NotAvailable
		val suffix = installerSuffix()
		val selected = release.assets.singleOrNull { it.name.endsWith(suffix, ignoreCase = true) } ?: error("This release has no $suffix installer. Download it from GitHub instead.")
		val update = UpdateRelease(
			version = release.tag.removePrefix("v"),
			tag = release.tag,
			versionCode = remote.versionCode,
			changeLogMarkDown = release.body.orEmpty().normalizeReleaseBody().resolveLinks(),
		)
		asset = selected
		updateRelease = update
		return AppUpdate.Available(release = update)
	}

	private fun installerSuffix(): String = when {
		isWindows -> ".msi"
		pacmanInstaller != null -> PacmanUpdateInstaller.PACKAGE_SUFFIX
		else -> ".deb"
	}
}
