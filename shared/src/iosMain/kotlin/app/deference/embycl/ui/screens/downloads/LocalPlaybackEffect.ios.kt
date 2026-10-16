package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.deference.embycl.domain.model.download.*

actual val localPlaybackSupported = false
@Composable
actual fun LocalPlaybackEffect(request: LocalPlaybackRequest?, onResult: (String, LocalPlaybackResult) -> Unit) {
    LaunchedEffect(request?.sessionId) {
        request?.let { onResult(it.sessionId, LocalPlaybackResult(error = LocalPlayerError.UNSUPPORTED_PLATFORM)) }
    }
}
