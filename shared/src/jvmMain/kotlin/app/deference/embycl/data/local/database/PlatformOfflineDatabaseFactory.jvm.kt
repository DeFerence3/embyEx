package app.deference.embycl.data.local.database

import androidx.room.Room
import org.koin.core.annotation.Single
import java.io.File

@Single(binds = [OfflineDatabaseFactory::class])
class PlatformOfflineDatabaseFactory : OfflineDatabaseFactory {
    private val database by lazy {
        val directory = offlineDataDirectory()
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create offline database directory." }
        Room.databaseBuilder<OfflineDatabase>(
            name = File(directory, "offline.db").absolutePath,
            factory = { OfflineDatabaseConstructor.initialize() },
        ).buildOfflineDatabase()
    }

    override fun open(): OfflineDatabase = database
}

internal fun offlineDataDirectory(
    os: String = System.getProperty("os.name"),
    home: String = System.getProperty("user.home"),
    environment: (String) -> String? = System::getenv,
): File {
    fun absoluteEnvironment(name: String) = environment(name)?.takeIf { it.isNotBlank() }
        ?.let(::File)?.takeIf { it.isAbsolute }
    val base = when {
        os.startsWith("Windows", ignoreCase = true) -> absoluteEnvironment("LOCALAPPDATA") ?: File(home, "AppData/Local")
        os.startsWith("Mac", ignoreCase = true) -> File(home, "Library/Application Support")
        else -> absoluteEnvironment("XDG_DATA_HOME") ?: File(home, ".local/share")
    }
    return File(base, "embyEx")
}
