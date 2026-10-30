package app.deference.embcl.crash

/**
 * copied from https://github.com/abdallahmehiz/mpvKt/tree/main/app/src/main/java/live/mehiz/mpvkt/presentation/crash
 */

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.coroutineScope
import app.deference.embcl.BuildConfig
import app.deference.embcl.MainActivity
import app.deference.embycl.core.utils.buildCrashIssueUri
import app.deference.embycl.core.utils.constructEnv
import app.deference.embycl.crash.CrashScreen
import app.deference.embycl.ui.theme.EmbympvTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Date
import app.deference.embycl.BuildConfig as CommonBuildConfig

class CrashActivity : ComponentActivity() {
	
    private var logcat by mutableStateOf<String?>(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val exceptionString = intent.getStringExtra("exception").orEmpty()
        lifecycle.coroutineScope.launch {
            logcat = try {
                withContext(context = Dispatchers.IO) {
                    collectLogcat()
                }
            } catch (_: IOException) {
                ""
            } catch (_: SecurityException) {
                ""
            }
        }
        setContent {
            EmbympvTheme {
				val clipboardManager = LocalClipboard.current
				val localUriManager = LocalUriHandler.current
                var reportFailed by remember { mutableStateOf(value = false) }
				val scope = rememberCoroutineScope()
				val environment = getEnvironment()
                CrashScreen(
                    exceptionString = exceptionString,
                    logcat = logcat,
                    onReport = {
                        reportFailed = false
                        val issueUrl = buildCrashIssueUri(
                            exceptionString = exceptionString,
                            environment = environment,
                        )
						val issueUri = issueUrl.toString()
                        try {
							val clipData = ClipData.newPlainText("Emby Ex Raw Crash Log", exceptionString)
							scope.launch {
								clipboardManager.setClipEntry(ClipEntry(clipData))
							}
							localUriManager.openUri(issueUri)
                        } catch (_: ActivityNotFoundException) {
                            reportFailed = true
                        } catch (_: SecurityException) {
                            reportFailed = true
                        }
                    },
                    onCopyLogs = {
						val clipData = ClipData.newPlainText("EmbyEx Crash Log", concatLogs(
							deviceInfo = constructEnv(environment),
							crashLogs = exceptionString,
							logcat = logcat.orEmpty(),
						))
						scope.launch {
							clipboardManager.setClipEntry(ClipEntry(clipData))
						}
                    },
                    onRestartApp = {
                        finish()
                        startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                    },
                )
            }
        }
    }

    private fun collectLogcat(): String {
        val runtime = Runtime.getRuntime()
        val logs = runtime.exec("logcat -d").inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
        // Clear logcat so it does not pollute subsequent crashes.
        runtime.exec("logcat -c")
        return logs
    }

    private fun concatLogs(
        deviceInfo: String,
        crashLogs: String,
        logcat: String,
    ): String {
        return """
$deviceInfo

Exception:
$crashLogs

Logcat:
$logcat
    """.trimIndent()
    }

}

private fun getEnvironment(): List<Pair<String, String>>{
	return listOf(
		"App version" to "${BuildConfig.VERSION_NAME} (${CommonBuildConfig.GIT_SHA})",
		"Android version" to "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})",
		"Device brand" to "${Build.BRAND}",
		"Device manufacturer" to "${Build.MANUFACTURER}",
		"Device model" to "${Build.MODEL} (${Build.DEVICE})",
		"Language" to Locale.current.toLanguageTag(),
		"Timestamp (UTC)" to "${Date()}",
	)
}