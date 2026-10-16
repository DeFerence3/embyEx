package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.OutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface OutboxDao {
    @Upsert suspend fun saveOutbox(row: OutboxEntity)

    @Query("SELECT * FROM offline_outbox WHERE serverId=:server AND userId=:user ORDER BY createdAt, itemId")
    fun observeOutbox(server: String, user: String): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM offline_outbox WHERE serverId=:server AND userId=:user AND itemId=:item")
    suspend fun outbox(server: String, user: String, item: String): OutboxEntity?

    @Query("DELETE FROM offline_outbox WHERE serverId=:server AND userId=:user AND itemId=:item AND revision=:revision")
    suspend fun deleteAcknowledged(server: String, user: String, item: String, revision: Long): Int
}
