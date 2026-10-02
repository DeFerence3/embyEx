package app.deference.embycl.data.repository

import app.deference.embycl.domain.repository.AppRepo
import org.koin.core.annotation.Single

@Single
class AppRepoImpl(private val appUpdater: AppUpdater) : AppRepo {
    override val updateState = appUpdater.state
    override fun checkForUpdates(userInitiated: Boolean) = appUpdater.checkForUpdates(userInitiated)
    override fun downloadUpdate() = appUpdater.download()
    override fun installUpdate() = appUpdater.install()
    override fun dismissUpdate() = appUpdater.dismiss()
}
