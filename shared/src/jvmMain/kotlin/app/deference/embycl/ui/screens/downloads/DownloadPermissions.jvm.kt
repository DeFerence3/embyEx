package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable

@Composable actual fun rememberDownloadPermissionRequest(): ((Boolean) -> Unit) -> Unit = { it(true) }
actual val downloadMeteringSupported = false
