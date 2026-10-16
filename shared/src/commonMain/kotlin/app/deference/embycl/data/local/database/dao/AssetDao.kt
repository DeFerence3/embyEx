package app.deference.embycl.data.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.deference.embycl.data.local.database.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AssetDao {
    @Upsert suspend fun saveAsset(row: AssetEntity)

    @Query("""SELECT a.* FROM offline_assets AS a JOIN offline_downloads AS d ON a.downloadId=d.id
        WHERE d.serverId=:server AND d.userId=:user AND d.itemId=:item AND d.sourceId=:source ORDER BY a.assetId""")
    fun observeAssets(server: String, user: String, item: String, source: String): Flow<List<AssetEntity>>
}
