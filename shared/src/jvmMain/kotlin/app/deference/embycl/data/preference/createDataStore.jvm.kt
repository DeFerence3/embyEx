package app.deference.embycl.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import java.io.File

fun createDataStore(): DataStore<Preferences> = createDataStore(
	producePath = {
		val os = System.getProperty("os.name").lowercase()
		val baseDir = when {
			os.contains("win") -> System.getenv("APPDATA") ?: System.getProperty("user.home")
			os.contains("mac") -> System.getProperty("user.home") + "/Library/Application Support"
			else -> System.getenv("XDG_CONFIG_HOME") ?: (System.getProperty("user.home") + "/.config")
		}
		
		val appDir = File(baseDir, "embyEx")
		if (!appDir.exists()) {
			appDir.mkdirs()
		}
		
		File(appDir, dataStoreFileName).absolutePath },
)

actual val gello: String
	get() = TODO("Not yet implemented")