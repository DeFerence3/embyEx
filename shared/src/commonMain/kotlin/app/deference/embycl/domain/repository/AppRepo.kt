package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.update.AppUpdate
import kotlinx.coroutines.flow.Flow

interface AppRepo {
    suspend fun checkForUpdates(userInitiated: Boolean = true): Result<AppUpdate>
    fun downloadUpdate(): Flow<AppUpdate>
    suspend fun installUpdate(): AppUpdate
    fun onAppResumed(current: AppUpdate): AppUpdate
}
