package app.deference.embycl.data.local.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class EmbyPreference(private val dataStore: DataStore<Preferences>) {

    /** Commit account credentials/identity together, including removal, across process death. */
    fun saveStrings(values: Map<String, String?>) = runBlocking {
        dataStore.edit { preferences ->
            values.forEach { (name, value) ->
                val key = stringPreferencesKey(name)
                if (value == null) preferences.remove(key) else preferences[key] = value
            }
        }
        Unit
    }

    fun observeBoolean(key: String) = dataStore.data.map { it[booleanPreferencesKey(key)] ?: false }.distinctUntilChanged()
    suspend fun setBoolean(key: String, value: Boolean) { dataStore.edit { it[booleanPreferencesKey(key)] = value } }
	
	fun <T> save(key: String, value: T) {
		runBlocking {
			dataStore.edit { preferences ->
				when (value) {
					is Boolean -> preferences[booleanPreferencesKey(key)] = value
					is Float -> preferences[floatPreferencesKey(key)] = value
					is Long -> preferences[longPreferencesKey(key)] = value
					is String -> preferences[stringPreferencesKey(key)] = value
					is Int -> preferences[intPreferencesKey(key)] = value
				}
			}
		}
	}
	
	fun getBoolean(key: String): Boolean {
		return runBlocking {
			val preferences = dataStore.data.first()
			preferences[booleanPreferencesKey(key)] ?: false
		}
	}
	
	fun getString(key: String): String? {
		return runBlocking {
			val preferences = dataStore.data.first()
			preferences[stringPreferencesKey(key)]
		}
	}
	
	fun getString(key: String, default: String): String {
		return runBlocking {
			val preferences = dataStore.data.first()
			preferences[stringPreferencesKey(key)] ?: default
		}
	}
	
	fun getInt(key: String): Int? {
		return runBlocking {
			val preferences = dataStore.data.first()
			preferences[intPreferencesKey(key)]
		}
	}
	
	fun remove(key: String) {
		runBlocking {
			dataStore.edit { preferences ->
				// Since we don't know the type, we try to remove it from all possible types
				// or we can use a generic approach if DataStore supports it.
				// In Preferences DataStore, we need the Key object.
				// However, we can use the fact that we can iterate and find keys with the same name.
				val keysToRemove = preferences.asMap().keys.filter { it.name == key }
				keysToRemove.forEach { preferences.remove(it) }
			}
		}
	}
}
