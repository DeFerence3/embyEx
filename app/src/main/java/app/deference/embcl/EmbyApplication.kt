package app.deference.embcl

import android.app.Application
import app.deference.embcl.core.di.initKoin
import io.kotzilla.generated.monitoring
import org.koin.android.ext.koin.androidContext

class EmbyApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    initKoin {
      androidContext(this@EmbyApplication)
      monitoring()
    }
  }
}
