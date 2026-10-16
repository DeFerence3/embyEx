package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.PlaybackEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PlaybackDao {
    @Upsert suspend fun savePlayback(row: PlaybackEntity)

    @Query("SELECT * FROM offline_playback WHERE serverId=:server AND userId=:user AND itemId=:item")
    suspend fun playback(server: String, user: String, item: String): PlaybackEntity?

    @Query("SELECT * FROM offline_playback WHERE serverId=:server AND userId=:user AND itemId=:item")
    fun observePlayback(server: String, user: String, item: String): Flow<PlaybackEntity?>
}
