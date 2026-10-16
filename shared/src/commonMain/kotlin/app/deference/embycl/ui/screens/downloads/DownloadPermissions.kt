package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable

/** Notification permission only; app-private download storage requires no storage permission. */
@Composable
expect fun rememberDownloadPermissionRequest(): ((Boolean) -> Unit) -> Unit

expect val downloadMeteringSupported: Boolean
