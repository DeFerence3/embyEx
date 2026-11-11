package app.deference.embycl.ui.core.network

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val LOCAL_NETWORK_PERMISSION_SDK = 37

@Composable
actual fun LocalNetworkPermissionEffect(onGranted: () -> Unit) {
    if (Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK) return
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) currentOnGranted()
    }
    LaunchedEffect(launcher) {
        if (requested) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_LOCAL_NETWORK,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requested = true
            launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
