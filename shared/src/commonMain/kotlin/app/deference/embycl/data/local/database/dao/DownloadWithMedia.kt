package app.deference.embycl.data.local.database.dao

import androidx.room.Embedded
import app.deference.embycl.data.local.database.entity.DownloadEntity

internal data class DownloadWithMedia(
	@Embedded val download: DownloadEntity,
	val snapshotJson: String,
	val serverObservedAt: Long,
	val serverUnavailable: Boolean
)