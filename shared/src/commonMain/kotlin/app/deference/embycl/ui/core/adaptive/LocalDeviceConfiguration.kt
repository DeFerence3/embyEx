package app.deference.embycl.ui.core.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import app.deference.embycl.ui.core.LocalDeviceConfiguration

@Composable
fun ProvideDeviceConfiguration(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = if (constraints.hasBoundedHeight) maxHeight else maxWidth
        val configuration = DeviceConfiguration.fromWindowSizeClass(
            windowSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(
                widthDp = maxWidth.value,
                heightDp = height.value,
            ),
        )
        CompositionLocalProvider(
            LocalDeviceConfiguration provides configuration,
            content = content,
        )
    }
}

@Composable
fun PreviewDeviceConfiguration(
    configuration: DeviceConfiguration,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDeviceConfiguration provides configuration,
        content = content,
    )
}
