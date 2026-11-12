package app.deference.embycl.ui.core.network

import androidx.compose.runtime.Composable

/** iOS asks for local network access itself on first use; there is no runtime permission to request here. */
@Composable
actual fun LocalNetworkPermissionEffect(onGranted: () -> Unit) = Unit
