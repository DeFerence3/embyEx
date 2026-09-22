package app.deference.embycl

import android.os.Build

class AndroidPlatform : Platform {
	override val name: String = "Android ${Build.VERSION.SDK_INT}"
	override val platform: Platforms = Platforms.ANDROID
}

actual fun getPlatform(): Platform = AndroidPlatform()