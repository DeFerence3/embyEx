package app.deference.embycl.data.repository

import app.deference.embycl.domain.model.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/** iOS builds are not distributed through GitHub releases, so there is nothing to update; it stays idle. */
@Single
actual class AppUpdater {
    private val _state = MutableStateFlow(UpdateState())
    actual val state: StateFlow<UpdateState> = _state.asStateFlow()

    actual fun checkForUpdates(userInitiated: Boolean) = Unit

    actual fun download() = Unit

    actual fun install() = Unit

    actual fun dismiss() = Unit
}
