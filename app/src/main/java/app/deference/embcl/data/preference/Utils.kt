package app.deference.embcl.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

private fun createDataStore(producePath: () -> String): DataStore<Preferences> =
	PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

private const val dataStoreFileName = "emby_mpv.preferences_pb"

fun createDataStore(context: Context): DataStore<Preferences> {
	return createDataStore {
		context.filesDir.resolve(dataStoreFileName).absolutePath
	}
}