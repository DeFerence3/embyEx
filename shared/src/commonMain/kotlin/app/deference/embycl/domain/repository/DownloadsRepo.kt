package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.DownloadKey
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.model.download.DownloadResult
import app.deference.embycl.domain.model.download.DownloadSource

/** Single entry point for saving and removing media. Connection, permissions, queueing and playback guards live here. */
interface DownloadsRepo {
    /** Resolves downloadable sources for [item]. */
    suspend fun download(item: EmbyItem): DownloadRequest
    /** Enqueues the source picked from a [DownloadRequest.Choose]. */
    suspend fun confirm(key: DownloadKey): DownloadResult
    /** Removes the saved copy of media [itemId]. */
    suspend fun remove(itemId: String): DownloadResult
}
