package app.deference.embcl

import android.app.Application
import app.deference.embycl.core.di.initKoin
import app.deference.embycl.core.session.Session
import app.deference.embycl.data.local.preference.EmbyPreference
import app.deference.embycl.data.local.preference.createDataStore
import app.deference.embycl.data.repository.download.AndroidDownloadScheduler
import app.deference.embycl.data.repository.download.DownloadQueue
import kotlinx.coroutines.*
import org.koin.mp.KoinPlatformTools
import org.koin.android.ext.koin.androidContext

class EmbyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	
	override fun onCreate() {
		val dataStore = createDataStore(this.applicationContext)
		Session.init(EmbyPreference(dataStore))
		super.onCreate()
		initKoin {
			androidContext(this@EmbyApplication)
		}
        // Register visibility tracking before any Activity starts. Scheduling itself is user-driven.
        KoinPlatformTools.defaultContext().get().get<AndroidDownloadScheduler>()
        applicationScope.launch {
            try { KoinPlatformTools.defaultContext().get().get<DownloadQueue>().recover() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { android.util.Log.e("Downloads", "Offline recovery failed; it will be retried before execution.") }
        }
	}
}
