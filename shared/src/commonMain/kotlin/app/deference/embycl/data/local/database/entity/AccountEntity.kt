package app.deference.embycl.data.local.database.entity

import androidx.room.Entity

@Entity(tableName = "offline_accounts", primaryKeys = ["serverId", "userId"])
internal data class AccountEntity(
	val serverId: String, val userId: String,
	val serverName: String, val userName: String, val updatedAt: Long,
)