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
import app.deference.embycl.ui.player.PlayerAspectRatio
import app.deference.embycl.ui.player.PlayerAspect

@Composable
internal fun AndroidPlayerAspectButton(
    aspect: PlayerAspect,
    enabled: Boolean,
    onModeChanged: (PlayerAspectRatio) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode by aspect.mode.collectAsState()
    AndroidPlayerButton(
        icon = when (mode) {
            PlayerAspectRatio.FIT -> Icons.Default.AspectRatio
            PlayerAspectRatio.STRETCH -> Icons.Default.CropFree
            PlayerAspectRatio.CROP -> Icons.Default.FitScreen
        },
        description = stringResource(resource = Res.string.player_aspect),
        onClick = {
            dispatchAction {
                val next = when (aspect.mode.value) {
                    PlayerAspectRatio.FIT -> PlayerAspectRatio.STRETCH
                    PlayerAspectRatio.STRETCH -> PlayerAspectRatio.CROP
                    PlayerAspectRatio.CROP -> PlayerAspectRatio.FIT
                }
                aspect.setMode(mode = next)
                onModeChanged(next)
            }
        },
        enabled = enabled,
        modifier = modifier,
    )
}
