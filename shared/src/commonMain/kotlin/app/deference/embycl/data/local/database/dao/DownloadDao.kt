package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DownloadDao {
    @Upsert suspend fun saveDownload(row: DownloadEntity)

    @Query("""SELECT d.*, m.snapshotJson, m.serverObservedAt, m.serverUnavailable FROM offline_downloads AS d
        JOIN offline_media AS m ON d.serverId=m.serverId AND d.userId=m.userId AND d.itemId=m.itemId
        WHERE d.serverId=:server AND d.userId=:user AND d.itemId=:item AND d.sourceId=:source""")
    suspend fun download(server: String, user: String, item: String, source: String): DownloadWithMedia?

    @Query("""SELECT d.*, m.snapshotJson, m.serverObservedAt, m.serverUnavailable FROM offline_downloads AS d
        JOIN offline_media AS m ON d.serverId=m.serverId AND d.userId=m.userId AND d.itemId=m.itemId
        WHERE d.serverId=:server AND d.userId=:user ORDER BY d.createdAt, d.id""")
    fun observeDownloads(server: String, user: String): Flow<List<DownloadWithMedia>>

    @Query("DELETE FROM offline_downloads WHERE serverId=:server AND userId=:user AND itemId=:item AND sourceId=:source AND id=:id AND revision=:revision")
    suspend fun deleteDownload(server: String, user: String, item: String, source: String, id: String, revision: Long): Int
}
