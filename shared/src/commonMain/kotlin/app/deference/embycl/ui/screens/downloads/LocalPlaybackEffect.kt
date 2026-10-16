package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import app.deference.embycl.domain.model.download.LocalPlaybackRequest
import app.deference.embycl.domain.model.download.LocalPlaybackResult

expect val localPlaybackSupported: Boolean

/** Mounted at the app root, independent of the current account's navigation stack. */
@Composable
expect fun LocalPlaybackEffect(
    request: LocalPlaybackRequest?,
    onResult: (String, LocalPlaybackResult) -> Unit,
)
