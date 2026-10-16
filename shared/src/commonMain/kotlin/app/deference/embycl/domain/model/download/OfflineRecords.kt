package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyUserData

data class OfflineAccountKey(val serverId: String, val userId: String) {
    init { require(serverId.isNotBlank() && userId.isNotBlank()) }
}

data class OfflineMediaKey(val account: OfflineAccountKey, val itemId: String) {
    init { require(itemId.isNotBlank()) }
}

fun DownloadKey.accountKey() = OfflineAccountKey(serverId, userId)
fun DownloadKey.mediaKey() = OfflineMediaKey(accountKey(), itemId)

/** Labels only. Connection addresses and credentials remain outside the offline catalog. */
data class OfflineAccount(val key: OfflineAccountKey, val serverName: String, val userName: String)

/** Last observed metadata; observePlayback is authoritative for local watch progress. */
data class OfflineMedia(val item: EmbyItem, val serverObservedAt: Long, val serverUnavailable: Boolean = false)

enum class LocalMediaState { NOT_READY, AVAILABLE, MISSING }

/** Including the local ID rejects callbacks from a removed-and-recreated download. */
data class DownloadVersion(val downloadId: String, val revision: Long) {
    init { require(downloadId.isNotBlank() && revision >= 0) }
}

data class StoredDownload(
    /** Generated local directory identity, unrelated to remote IDs or filenames. */
    val id: String,
    val task: DownloadTask,
    val source: DownloadSource,
    val probe: DownloadProbe,
    val media: OfflineMedia,
    val relativeMediaPath: String?,
    val localMediaState: LocalMediaState,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
) {
    val version get() = DownloadVersion(id, revision)
    /** The file layer must still check availability immediately before opening a file. */
    val isLocallyAvailable get() = task.status == DownloadStatus.COMPLETED && localMediaState == LocalMediaState.AVAILABLE

    fun change(
        task: DownloadTask = this.task,
        probe: DownloadProbe = this.probe,
        relativeMediaPath: String? = this.relativeMediaPath,
        localMediaState: LocalMediaState = this.localMediaState,
        source: DownloadSource = this.source,
    ) = DownloadChange(task, probe, relativeMediaPath, localMediaState, source)
}

data class DownloadChange(
    val task: DownloadTask,
    val probe: DownloadProbe,
    val relativeMediaPath: String?,
    val localMediaState: LocalMediaState,
    val source: DownloadSource,
)

enum class OfflineAssetKind { POSTER, BACKDROP, LOGO, SUBTITLE }
enum class OfflineAssetState { PENDING, READY, FAILED, MISSING }

data class OfflineAsset(
    /** Logical identity (poster, subtitle-3), never a remote URL. */
    val id: String,
    val kind: OfflineAssetKind,
    val state: OfflineAssetState,
    val relativePath: String? = null,
    val sizeBytes: Long? = null,
    val streamIndex: Int? = null,
) {
    init {
        require(id.matches(Regex("[a-zA-Z0-9_-]+")))
        require(sizeBytes == null || sizeBytes >= 0)
        require(streamIndex == null || streamIndex >= 0)
        require(kind != OfflineAssetKind.SUBTITLE || streamIndex != null)
        require(state != OfflineAssetState.READY || (relativePath != null && sizeBytes != null && sizeBytes > 0))
    }
}

data class OfflinePlayback(
    val key: OfflineMediaKey,
    val positionTicks: Long,
    val played: Boolean,
    val revision: Long,
    val acknowledgedRevision: Long,
    /** Server state before pending edits, or the last accepted sync acknowledgement. */
    val serverBaseline: EmbyUserData?,
    val updatedAt: Long,
)

data class PendingPlaybackSync(val playback: OfflinePlayback, val createdAt: Long, val journal: PlaybackSyncJournal = PlaybackSyncJournal())
