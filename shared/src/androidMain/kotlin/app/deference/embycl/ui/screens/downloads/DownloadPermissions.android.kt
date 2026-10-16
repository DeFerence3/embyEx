package app.deference.embycl.ui.screens.downloads

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberDownloadPermissionRequest(): ((Boolean) -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val callback = pending; pending = null; callback?.invoke(granted)
    }
    return { callback ->
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) callback(true)
        else if (pending == null) { pending = callback; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
actual val downloadMeteringSupported = true
