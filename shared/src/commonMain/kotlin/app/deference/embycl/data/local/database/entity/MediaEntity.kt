package app.deference.embycl.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "offline_media", primaryKeys = ["serverId", "userId", "itemId"],
	foreignKeys = [ForeignKey(AccountEntity::class, ["serverId", "userId"], ["serverId", "userId"], onDelete = ForeignKey.CASCADE)],
)
internal data class MediaEntity(
	val serverId: String, val userId: String, val itemId: String,
	val title: String, val searchText: String, val snapshotJson: String, val serverObservedAt: Long,
	@androidx.room.ColumnInfo(defaultValue = "0") val serverUnavailable: Boolean = false,
)