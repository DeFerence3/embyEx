package app.deference.embycl.data.local.database

import app.deference.embycl.domain.repository.*
import org.koin.core.annotation.Single

/** iOS file transfer/playback is outside the initial Android/desktop scope. */
@Single(binds = [OfflineFileStore::class])
class PlatformOfflineFileStore : OfflineFileStore {
    override suspend fun <T> withSyncLease(block: suspend () -> T): T =
        throw OfflineStorageException(OfflineStorageFailure.UNSUPPORTED)
    override suspend fun <T> withQueueLease(block: suspend () -> T): T =
        throw OfflineStorageException(OfflineStorageFailure.UNSUPPORTED)
    override suspend fun <T> withDownload(downloadId: String, block: suspend (DownloadFiles) -> T): T =
        throw OfflineStorageException(OfflineStorageFailure.UNSUPPORTED)
    override suspend fun downloadIds(): List<String> = throw OfflineStorageException(OfflineStorageFailure.UNSUPPORTED)
}
