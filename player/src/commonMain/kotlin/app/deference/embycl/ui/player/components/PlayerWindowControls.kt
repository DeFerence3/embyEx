package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_exit_fullscreen
import embyex.player.generated.resources.player_fullscreen
import embyex.player.generated.resources.player_pip
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlayerWindowControls(
    fullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
    onPictureInPicture: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (onPictureInPicture != null) PlayerIconButton(
            icon = Icons.Default.PictureInPictureAlt,
            description = stringResource(resource = Res.string.player_pip),
            onClick = onPictureInPicture,
        )
        if (onToggleFullscreen != null) PlayerIconButton(
            icon = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            description = stringResource(
                resource = if (fullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen,
            ),
            onClick = onToggleFullscreen,
        )
    }
}
