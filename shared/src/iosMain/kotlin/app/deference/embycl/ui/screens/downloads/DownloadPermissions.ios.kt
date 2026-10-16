package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable

@Composable actual fun rememberDownloadPermissionRequest(): ((Boolean) -> Unit) -> Unit = { it(false) }
actual val downloadMeteringSupported = false
