package app.deference.embycl.ui.player.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_aspect
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.features.AspectRatioMode
import org.openani.mediamp.features.VideoAspectRatio

@Composable
internal fun AndroidPlayerAspectButton(
    aspect: VideoAspectRatio,
    enabled: Boolean,
    onModeChanged: (AspectRatioMode) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode by aspect.mode.collectAsState()
    AndroidPlayerButton(
        icon = when (mode) {
            AspectRatioMode.FIT -> Icons.Default.AspectRatio
            AspectRatioMode.STRETCH -> Icons.Default.CropFree
            AspectRatioMode.CROP -> Icons.Default.FitScreen
        },
        description = stringResource(resource = Res.string.player_aspect),
        onClick = {
            dispatchAction {
                val next = when (aspect.mode.value) {
                    AspectRatioMode.FIT -> AspectRatioMode.STRETCH
                    AspectRatioMode.STRETCH -> AspectRatioMode.CROP
                    AspectRatioMode.CROP -> AspectRatioMode.FIT
                }
                aspect.setMode(mode = next)
                onModeChanged(next)
            }
        },
        enabled = enabled,
        modifier = modifier,
    )
}
