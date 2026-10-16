package app.deference.embycl.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "offline_playback", primaryKeys = ["serverId", "userId", "itemId"],
	foreignKeys = [ForeignKey(MediaEntity::class, ["serverId", "userId", "itemId"], ["serverId", "userId", "itemId"], onDelete = ForeignKey.CASCADE)],
)
internal data class PlaybackEntity(
	val serverId: String, val userId: String, val itemId: String,
	val positionTicks: Long, val played: Boolean, val revision: Long, val acknowledgedRevision: Long,
	val serverBaselineJson: String?, val updatedAt: Long,
)