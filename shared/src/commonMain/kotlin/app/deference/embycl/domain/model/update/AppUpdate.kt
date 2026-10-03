package app.deference.embycl.domain.model.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRelease(
	val version: String,
	val tag: String,
	val versionCode: Long,
	val changeLogMarkDown: String
)

sealed interface AppUpdate {
    data object Idle : AppUpdate
    data object Checking : AppUpdate
    data object NotAvailable : AppUpdate
    data class Available(val release: UpdateRelease) : AppUpdate
    data class Downloading(val release: UpdateRelease, val downloaded: Long, val total: Long) : AppUpdate {
        val fraction: Float? get() = if (total > 0) (downloaded.toDouble() / total).coerceIn(0.0, 1.0).toFloat() else null
    }
    data class ReadyToInstall(val release: UpdateRelease) : AppUpdate
    data class AwaitingPermission(val release: UpdateRelease) : AppUpdate
    data class InstallerLaunched(val release: UpdateRelease) : AppUpdate
    data class Failed(val message: String, val stage: UpdateStage, val release: UpdateRelease? = null) : AppUpdate
}

enum class UpdateStage { Check, Download, Install }

/** Visibility is independent of progress, so dismissing does not cancel or reopen a download. */
data class UpdateState(
	val update: AppUpdate = AppUpdate.Idle,
	val dialogVisible: Boolean = false
)
