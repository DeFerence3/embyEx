package app.deference.embycl.data.repository

import app.deference.embycl.domain.ghrelease.GithubRelease
import app.deference.embycl.domain.model.update.AppUpdate
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
expect class AppUpdater {
    suspend fun checkForUpdates(
        release: GithubRelease,
        userInitiated: Boolean = true,
    ): AppUpdate

    fun download(): Flow<AppUpdate>

    suspend fun install(): AppUpdate

    fun onAppResumed(current: AppUpdate): AppUpdate
}
