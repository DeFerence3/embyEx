package app.deference.embycl.data.repository

import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.repository.AppRepo
import app.deference.embycl.domain.repository.GithubRepo
import app.deference.embycl.ui.core.SettingsPreferences
import kotlinx.coroutines.CancellationException
import org.koin.core.annotation.Single

@Single
class AppRepoImpl(
    private val appUpdater: AppUpdater,
    private val githubRepo: GithubRepo,
) : AppRepo {
    override suspend fun checkForUpdates(userInitiated: Boolean): Result<AppUpdate> = try {
        val release = githubRepo.getLatestRelease(prerelease = SettingsPreferences.betaUpdatesEnabled)
        Result.success(
            appUpdater.checkForUpdates(
                release = release,
                userInitiated = userInitiated,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun downloadUpdate() = appUpdater.download()

    override suspend fun installUpdate() = appUpdater.install()

    override fun onAppResumed(current: AppUpdate) = appUpdater.onAppResumed(current = current)
}
