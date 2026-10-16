package app.deference.embycl.data.local.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.deference.embycl.data.local.database.dao.AccountDao
import app.deference.embycl.data.local.database.dao.AssetDao
import app.deference.embycl.data.local.database.dao.DownloadDao
import app.deference.embycl.data.local.database.dao.MediaDao
import app.deference.embycl.data.local.database.dao.OutboxDao
import app.deference.embycl.data.local.database.dao.PlaybackDao
import app.deference.embycl.data.local.database.entity.AccountEntity
import app.deference.embycl.data.local.database.entity.AssetEntity
import app.deference.embycl.data.local.database.entity.DownloadEntity
import app.deference.embycl.data.local.database.entity.MediaEntity
import app.deference.embycl.data.local.database.entity.OutboxEntity
import app.deference.embycl.data.local.database.entity.PlaybackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
		AccountEntity::class,
		MediaEntity::class,
		DownloadEntity::class,
        AssetEntity::class,
		PlaybackEntity::class,
		OutboxEntity::class],
    version = 1,
	exportSchema = true,
)
@ConstructedBy(OfflineDatabaseConstructor::class)
abstract class OfflineDatabase : RoomDatabase() {
    internal abstract val mediaDao: MediaDao
    internal abstract val downloadDao: DownloadDao
    internal abstract val assetDao: AssetDao
    internal abstract val playbackDao: PlaybackDao
    internal abstract val outboxDao: OutboxDao
    internal abstract val accountDao: AccountDao
}

@Suppress("KotlinNoActualForExpect")
expect object OfflineDatabaseConstructor : RoomDatabaseConstructor<OfflineDatabase> {
    override fun initialize(): OfflineDatabase
}

/** Register future migrations here. Never replace offline data on a schema mismatch. */
fun RoomDatabase.Builder<OfflineDatabase>.buildOfflineDatabase(): OfflineDatabase =
	setDriver(BundledSQLiteDriver())
		.setQueryCoroutineContext(Dispatchers.IO).build()

fun interface OfflineDatabaseFactory {
    /** One shared instance per app process; opening is lazy until a repository is used. */
    fun open(): OfflineDatabase
}
