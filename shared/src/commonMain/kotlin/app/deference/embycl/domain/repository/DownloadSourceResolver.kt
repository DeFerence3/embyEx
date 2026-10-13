package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.download.DownloadConnection
import app.deference.embycl.domain.model.download.DownloadResolution

interface DownloadSourceResolver {
    /** Null source selects the only supported source, or asks the user to choose. */
    suspend fun resolve(
        connection: DownloadConnection,
        itemId: String,
        mediaSourceId: String? = null,
    ): DownloadResolution
}
