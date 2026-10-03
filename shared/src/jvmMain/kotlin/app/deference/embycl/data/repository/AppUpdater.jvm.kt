package app.deference.embycl.data.repository

import app.deference.embycl.BuildConfig
import app.deference.embycl.core.networking.ResponseHandler.isSuccess
import app.deference.embycl.core.utils.Log
import app.deference.embycl.core.utils.resolveLinks
import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateRelease
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.domain.model.update.UpdateState
import dev.nucleusframework.updater.NucleusUpdater
import dev.nucleusframework.updater.UpdateResult
import dev.nucleusframework.updater.provider.GitHubProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import java.net.http.HttpClient as JavaHttpClient

@Single
actual class AppUpdater(
	@Named("download")
	private val client: HttpClient
) {
	private val owner = "DeFerence3"
	private val repo = "embyEx"
	
	internal val updateJson = Json { ignoreUnknownKeys = true }
	
	private var result: UpdateResult.Available? = null
	private var updateRelease: UpdateRelease? = null
	private val _state = MutableStateFlow(UpdateState())
	private val gitHubProvider = GitHubProvider(owner = owner, repo = repo)
	private var installer: File? = null
	private val javaHttpClient = JavaHttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(20))
		.followRedirects(JavaHttpClient.Redirect.NORMAL)
		.build()
	private val updater = NucleusUpdater {
		provider = gitHubProvider
		currentVersion = BuildConfig.VERSION_NAME
		executableType = "msi"
		httpClient = javaHttpClient
	}
	
	val errorHandler = CoroutineExceptionHandler { context, exception ->
		val appUpdate = AppUpdate.Failed(exception.message ?: "Could not check for updates. Check your connection and try again.", UpdateStage.Check)
		publish(appUpdate)
	}
	
	private val scope = CoroutineScope(Dispatchers.IO + errorHandler)
	
	actual val state: StateFlow<UpdateState> = _state.asStateFlow()
	
	actual fun checkForUpdates(userInitiated: Boolean) {
		if (userInitiated) _state.update { it.copy(dialogVisible = true) }
		Log.i("AppUpdater.jvm"){ "Checkingforupdates---> $userInitiated" }
		scope.launch {
			val reveal = !userInitiated
			if(_state.value.update !is AppUpdate.Downloading){
				publish(AppUpdate.Checking,userInitiated)
			}
			when (val result = updater.checkForUpdates()) {
				is UpdateResult.Available -> {
					Log.i("AppUpdater.jvm"){ "UpdateAvailable---> $result" }
					val update = AppUpdate.Available(
						UpdateRelease(
							result.info.version,
							versionCode = 0,
							tag = "v${result.info.version}",
							changeLogMarkDown = getChangeLog("v${result.info.version}")
								.resolveLinks(),
						).also {
							updateRelease = it
							this@AppUpdater.result = result
						}
					)
					if(_state.value.update is AppUpdate.Downloading){
						publish(_state.value.update,reveal)
					}else{
						publish(update,reveal)
					}
				}
				UpdateResult.NotAvailable -> publish(AppUpdate.NotAvailable)
				is UpdateResult.Error -> {
					publish(AppUpdate.Failed(result.exception.message ?: "Could not check for updates. Check your connection and try again.", UpdateStage.Check))
					result.exception.cause?.printStackTrace()
				}
			}
		}
	}
	
	@OptIn(FlowPreview::class)
	actual fun download() {
		scope.launch {
			result?.let { res ->
				updateRelease?.let { release ->
					publish(AppUpdate.Downloading(release, 0, 0))
					updater.downloadUpdate(res.info)
						.debounce(1.seconds)
						.catch {
							publish(AppUpdate.Failed(it.message ?: "Could not download update. Check your connection and try again.", UpdateStage.Download))
							it.printStackTrace()
						}
						.collect{ progress ->
							progress.file?.let { installer = it }
							publish(AppUpdate.Downloading(release, progress.bytesDownloaded, progress.totalBytes))
						}
					install()
				}
			}
		}
	}
	
	actual fun install() {
		installer?.let(updater::installAndRestart)
	}
	
	actual fun dismiss() {
		_state.update { it.copy(dialogVisible = false) }
	}
	
	private suspend fun getChangeLog(tag: String): String{
		val url = "https://api.github.com/repos/$owner/$repo/releases/tags/$tag"
		val response = client.get(url) {
			header("Accept", "application/vnd.github+json")
			timeout { requestTimeoutMillis = 20_000 }
		}
		check(response.isSuccess) {
			if (response.status.value in listOf(403, 429)) "GitHub temporarily refused the update check. Try again later."
			else "Could not check for updates (HTTP ${response.status.value})."
		}
		val responseBody = response.bodyAsText()
		return updateJson.decodeFromString<GithubRelease>(responseBody).body ?: ""
	}
	
	private fun publish(update: AppUpdate, reveal: Boolean = false) {
		Log.i("AppUpdater.jvm"){ "Publishing---> $update - $reveal" }
		_state.update { it.copy(update = update, dialogVisible = it.dialogVisible || reveal) }
	}
}
