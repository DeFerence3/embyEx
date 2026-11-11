package app.deference.embycl.ui.core.network

import androidx.compose.runtime.Composable

// android-17 and up needs permission to access local network......... ppphhewwwww
@Composable
expect fun LocalNetworkPermissionEffect(onGranted: () -> Unit)
