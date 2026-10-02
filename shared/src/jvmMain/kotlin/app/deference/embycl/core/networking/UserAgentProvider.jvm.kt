package app.deference.embycl.core.networking

import app.deference.embycl.BuildConfig
import org.koin.core.annotation.Single

@Single
actual class UserAgentProvider {
	actual fun getUserAgent(): String {
		val appName = "EmbyEx Desktop"
		val versionName = BuildConfig.VERSION_NAME
		val osName = System.getProperty("os.name") ?: "Unknown"
		val osVersion = System.getProperty("os.appUpdate") ?: "Unknown"
		val osArch = System.getProperty("os.arch") ?: "Unknown"
		return "$appName/$versionName ($osName $osVersion; $osArch)"
	}
}