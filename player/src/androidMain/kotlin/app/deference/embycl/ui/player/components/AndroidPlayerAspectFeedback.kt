package app.deference.embycl.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.theme.PlayerColors
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_crop
import embyex.player.generated.resources.player_fit
import embyex.player.generated.resources.player_stretch
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.PlayerAspectRatio

@Composable
internal fun AndroidPlayerAspectFeedback(
    mode: PlayerAspectRatio?,
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    val shown = remember(key1 = trigger) { mode ?: PlayerAspectRatio.FIT }
    AnimatedVisibility(
        visible = mode != null,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(durationMillis = 100)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)),
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = PlayerColors.controlsSurface,
            contentColor = PlayerColors.content,
        ) {
            Text(
                text = stringResource(resource = when (shown) {
                    PlayerAspectRatio.FIT -> Res.string.player_fit
                    PlayerAspectRatio.STRETCH -> Res.string.player_stretch
                    PlayerAspectRatio.CROP -> Res.string.player_crop
                }),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
