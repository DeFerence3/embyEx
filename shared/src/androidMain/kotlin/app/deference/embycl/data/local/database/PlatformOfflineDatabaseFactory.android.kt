package app.deference.embycl.data.local.database

import android.content.Context
import androidx.room.Room
import org.koin.core.annotation.Single

@Single(binds = [OfflineDatabaseFactory::class])
class PlatformOfflineDatabaseFactory(context: Context) : OfflineDatabaseFactory {
    private val appContext = context.applicationContext
    private val database by lazy {
        Room.databaseBuilder<OfflineDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("offline.db").absolutePath,
            factory = { OfflineDatabaseConstructor.initialize() },
        ).buildOfflineDatabase()
    }

    override fun open(): OfflineDatabase = database
}
