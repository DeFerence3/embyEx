package app.deference.embcl

import android.app.Application
import app.deference.embcl.core.di.initKoin
import app.deference.embcl.core.session.Session
import app.deference.embcl.data.preference.EmbyPreference
import app.deference.embcl.data.preference.createDataStore
import org.koin.android.ext.koin.androidContext

class EmbyApplication : Application() {
	
	override fun onCreate() {
		val dataStore = createDataStore(this.applicationContext)
		Session.init(EmbyPreference(dataStore))
		super.onCreate()
		initKoin {
			androidContext(this@EmbyApplication)
		}
	}
}
