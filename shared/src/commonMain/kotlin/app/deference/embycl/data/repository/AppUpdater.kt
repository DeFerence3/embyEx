package app.deference.embycl.data.repository

import app.deference.embycl.domain.model.update.UpdateState
import kotlinx.coroutines.flow.StateFlow

expect class AppUpdater {
    val state: StateFlow<UpdateState>
    fun checkForUpdates(userInitiated: Boolean = true)
    fun download()
    fun install()
    fun dismiss()
}
