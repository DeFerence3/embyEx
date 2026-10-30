package app.deference.embcl.crash

/**
 * copied from https://github.com/abdallahmehiz/mpvKt/tree/main/app/src/main/java/live/mehiz/mpvkt/presentation/crash
 */

import android.content.Context
import android.content.Intent
import app.deference.embycl.core.utils.Log
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val context: Context,
    private val activity: Class<*>
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
		Log.i("GlobalExceptionHandler"){ "CatchingException---> ${e.message}" }
        val intent = Intent(context, activity)
			.apply {
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
				putExtra("exception", e.stackTraceToString())
			}
		context.startActivity(intent)
		exitProcess(0)
    }
}
