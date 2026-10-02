package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.update.UpdateState
import kotlinx.coroutines.flow.StateFlow

interface AppRepo {
    val updateState: StateFlow<UpdateState>
    fun checkForUpdates(userInitiated: Boolean = true)
    fun downloadUpdate()
    fun installUpdate()
    fun dismissUpdate()
}
