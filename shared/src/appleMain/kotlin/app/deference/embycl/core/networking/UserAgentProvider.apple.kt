package app.deference.embycl.core.networking

import org.koin.core.annotation.Single
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

@Single
actual class UserAgentProvider {
	actual fun getUserAgent(): String {
		val mainBundle = NSBundle.mainBundle
		val appName = mainBundle.objectForInfoDictionaryKey("CFBundleName") as? String ?: "UnknownApp"
		val versionName = mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
		val device = UIDevice.currentDevice
		val osVersion = "${device.systemName} ${device.systemVersion}"
		val deviceModel = device.model
		
		return "$appName/$versionName ($osVersion; $deviceModel)"
	}
}
