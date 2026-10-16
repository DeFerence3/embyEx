package app.deference.embycl.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "offline_outbox", primaryKeys = ["serverId", "userId", "itemId"],
	foreignKeys = [ForeignKey(PlaybackEntity::class, ["serverId", "userId", "itemId"], ["serverId", "userId", "itemId"], onDelete = ForeignKey.CASCADE)],
)
internal data class OutboxEntity(
	val serverId: String,
	val userId: String,
	val itemId: String,
	val positionTicks: Long,
	val played: Boolean,
	val revision: Long,
	val acknowledgedRevision: Long,
	val serverBaselineJson: String?,
	val createdAt: Long,
	val updatedAt: Long,
	val syncJson: String? = null,
)