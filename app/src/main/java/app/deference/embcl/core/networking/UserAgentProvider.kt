package app.deference.embcl.core.networking

import android.content.Context
import android.os.Build
import org.koin.core.annotation.Single

@Single
class UserAgentProvider(private val context: Context) {
	
	fun getUserAgent(): String {
		val packageManager = context.packageManager
		val appName = context.applicationInfo.loadLabel(packageManager).toString()
		val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
		val versionName = packageInfo.versionName ?: "unknown"
		val osVersion = "Android ${Build.VERSION.RELEASE}"
		val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
		
		return "$appName/$versionName ($osVersion; $deviceModel)"
	}
}