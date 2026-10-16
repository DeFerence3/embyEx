package app.deference.embycl.data.local.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.deference.embycl.data.local.preference.createDataStore
import java.io.File

fun createDataStore(): DataStore<Preferences> = createDataStore(
	producePath = { File("embycl.preferences_pb").absolutePath },
)

actual val gello: String
	get() = TODO("Not yet implemented")