package app.deference.embycl.data.repository

import android.content.Context
import app.deference.embycl.domain.model.update.UpdateState
import app.deference.embycl.platform.AndroidUpdateController
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

@Single
actual class AppUpdater(context: Context) {
    private val controller = AndroidUpdateController(context.applicationContext)
    actual val state: StateFlow<UpdateState> = controller.state
    actual fun checkForUpdates(userInitiated: Boolean) = controller.checkForUpdates(userInitiated)
    actual fun download() = controller.download()
    actual fun install() = controller.install()
    actual fun dismiss() = controller.dismiss()
    fun onAppResumed() = controller.onAppResumed()
}
