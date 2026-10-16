package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface MediaDao {
    @Upsert suspend fun saveMedia(row: MediaEntity)

    @Query("SELECT * FROM offline_media WHERE serverId=:server AND userId=:user AND itemId=:item")
    fun observeMedia(server: String, user: String, item: String): Flow<MediaEntity?>

    @Query("SELECT * FROM offline_media WHERE serverId=:server AND userId=:user AND itemId=:item")
    suspend fun media(server: String, user: String, item: String): MediaEntity?

    @Query("UPDATE offline_media SET serverUnavailable=:unavailable WHERE serverId=:server AND userId=:user AND itemId=:item")
    suspend fun markMediaUnavailable(server: String, user: String, item: String, unavailable: Boolean)

    @Query("""SELECT * FROM offline_media AS m WHERE serverId=:server AND userId=:user
        AND instr(searchText, :query) > 0 AND EXISTS (SELECT 1 FROM offline_downloads AS d
        WHERE d.serverId=m.serverId AND d.userId=m.userId AND d.itemId=m.itemId AND d.status='COMPLETED')
        ORDER BY title, itemId""")
    fun search(server: String, user: String, query: String): Flow<List<MediaEntity>>
}
