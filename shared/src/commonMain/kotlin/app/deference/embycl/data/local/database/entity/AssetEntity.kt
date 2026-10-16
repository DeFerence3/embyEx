package app.deference.embycl.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
	tableName = "offline_assets", primaryKeys = ["downloadId", "assetId"],
	foreignKeys = [ForeignKey(DownloadEntity::class, ["id"], ["downloadId"], onDelete = ForeignKey.CASCADE)],
)
internal data class AssetEntity(
	val downloadId: String, val assetId: String, val kind: String, val state: String,
	val relativePath: String?, val sizeBytes: Long?, val streamIndex: Int?, val updatedAt: Long,
)