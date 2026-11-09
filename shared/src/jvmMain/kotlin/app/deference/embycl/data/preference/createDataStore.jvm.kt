package app.deference.embycl.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.deference.embycl.data.local.preference.createDataStore
import java.io.File

fun createDataStore(): DataStore<Preferences> = createDataStore(
	producePath = { File("embycl.preferences_pb").absolutePath },
)
