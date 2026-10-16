package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.DownloadKey
import app.deference.embycl.domain.model.download.DownloadRejection
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.model.download.DownloadResolution
import app.deference.embycl.domain.model.download.DownloadResult
import app.deference.embycl.domain.model.download.accountKey
import app.deference.embycl.domain.repository.DownloadEnvironment
import app.deference.embycl.domain.repository.DownloadScheduling
import app.deference.embycl.domain.repository.DownloadSourceResolver
import app.deference.embycl.domain.repository.DownloadsRepo
import app.deference.embycl.domain.repository.OfflineRepository
import app.deference.embycl.domain.repository.OfflineUiSession
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

@Single(binds = [DownloadsRepo::class])
class DownloadsRepoImpl(
    private val offline: OfflineRepository,
    private val controller: DownloadController,
    private val resolver: DownloadSourceResolver,
    private val environment: DownloadEnvironment,
    private val session: OfflineUiSession,
    private val playback: OfflinePlaybackCoordinator,
) : DownloadsRepo {

    override suspend fun download(item: EmbyItem): DownloadRequest {
        val connection = environment.connection.value ?: return DownloadRequest.Failed("Sign in again to download. Saved media remains available.")
        if (!session.connected.value) return DownloadRequest.Failed("Connect to your server to download this item.")
        return runCatching {
            when (val result = resolver.resolve(connection, item.id)) {
                is DownloadResolution.Ready -> DownloadRequest.Choose(result.item.name, listOf(result.source), result.source.key)
                is DownloadResolution.ChooseSource -> DownloadRequest.Choose(item.name, result.sources)
                is DownloadResolution.Rejected -> {
                    if (result.reason == DownloadRejection.AUTHENTICATION_REQUIRED) session.expire(connection)
                    DownloadRequest.Failed(rejected(result.reason))
                }
            }
        }.getOrElse{ DownloadRequest.Failed("Couldn't check available downloads. Try again.") }
    }

    override suspend fun confirm(key: DownloadKey): DownloadResult {
        val account = session.account.value?.takeIf { it.key == key.accountKey() } ?: return DownloadResult(false, "Sign in to download this item.")
        val connection = environment.connection.value ?: return DownloadResult(false, "Sign in again to download. Saved media remains available.")
		return runCatching {
            when (val result = resolver.resolve(connection, key.itemId, key.mediaSourceId)) {
                is DownloadResolution.Ready -> when (controller.enqueue(account, result)) {
                    DownloadScheduling.SCHEDULED -> DownloadResult(true)
                    DownloadScheduling.REQUIRES_USER_ACTION -> DownloadResult(true, "Your download is saved in the queue. Keep EmbyEx open and tap Resume queue to start it.")
                    DownloadScheduling.UNAVAILABLE -> DownloadResult(true, "Couldn't start background downloads. Your queue is saved; try Resume queue again.")
                }
                is DownloadResolution.Rejected -> {
                    if (result.reason == DownloadRejection.AUTHENTICATION_REQUIRED) session.expire(connection)
                    DownloadResult(false, rejected(result.reason))
                }
                else -> DownloadResult(false, "An original download isn't available for this item or version.")
            }
		}.getOrElse{ DownloadResult(false, "Couldn't add this download. Check storage and try again.") }
    }

    override suspend fun remove(itemId: String): DownloadResult {
        val account = session.account.value ?: return DownloadResult(false, "No account is signed in.")
		return runCatching {
            val row = offline.observeDownloads(account.key).first().firstOrNull { it.task.key.itemId == itemId }
                ?: return DownloadResult(false, "This download is no longer available.")
            if (playback.request.value?.downloadId == row.id) return DownloadResult(false, "Close the player before changing this download.")
            controller.queue.remove(row.task.key)
            DownloadResult(true)
        }.getOrElse{ DownloadResult(false, "Couldn't update this download. Check storage and try again.") }
    }

    private fun rejected(reason: DownloadRejection) = when (reason) {
        DownloadRejection.AUTHENTICATION_REQUIRED -> "Sign in again to download. Saved media remains available."
        DownloadRejection.ACCESS_DENIED, DownloadRejection.PERMISSION_UNKNOWN -> "This account doesn't have permission to download this item."
        DownloadRejection.NETWORK_UNAVAILABLE, DownloadRejection.SERVER_ERROR -> "The server is unavailable. Try connecting again."
        DownloadRejection.RATE_LIMITED -> "The server is busy. Try again later."
        DownloadRejection.WRONG_SERVER, DownloadRejection.WRONG_USER -> "This address or account doesn't match the saved server."
        else -> "An original download isn't available for this item or version."
    }
}
