package app.deference.embycl.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> {
	return createDataStore {
		context.filesDir.resolve(dataStoreFileName).absolutePath
	}
}

actual val gello: String
	get() = TODO("Not yet implemented")