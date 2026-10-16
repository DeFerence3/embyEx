package app.deference.embycl.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
	tableName = "offline_downloads", primaryKeys = ["id"],
	indices = [Index(value = ["serverId", "userId", "itemId", "sourceId"], unique = true), Index(value = ["serverId", "userId", "status"])],
	foreignKeys = [ForeignKey(MediaEntity::class, ["serverId", "userId", "itemId"], ["serverId", "userId", "itemId"], onDelete = ForeignKey.CASCADE)],
)
internal data class DownloadEntity(
	val id: String,
	val serverId: String,
	val userId: String,
	val itemId: String,
	val sourceId: String,
	val status: String,
	val taskJson: String,
	val sourceJson: String,
	val probeJson: String,
	val relativeMediaPath: String?,
	val localMediaState: String,
	val createdAt: Long,
	val updatedAt: Long,
	val revision: Long,
)