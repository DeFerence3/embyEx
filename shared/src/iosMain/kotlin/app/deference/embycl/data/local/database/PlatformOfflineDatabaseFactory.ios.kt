package app.deference.embycl.data.local.database

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Single
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Single(binds = [OfflineDatabaseFactory::class])
class PlatformOfflineDatabaseFactory : OfflineDatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    private val database by lazy {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSApplicationSupportDirectory, inDomain = NSUserDomainMask,
            appropriateForURL = null, create = true, error = null,
        ) ?: error("Cannot create offline database directory.")
        Room.databaseBuilder<OfflineDatabase>(
            name = "${checkNotNull(directory.path)}/offline.db",
            factory = { OfflineDatabaseConstructor.initialize() },
        ).buildOfflineDatabase()
    }

    override fun open(): OfflineDatabase = database
}
