package app.deference.embycl.ui.player

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_external_error
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun PlatformPlayerOverlay(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    window: PlayerWindowState,
    paused: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    surface: @Composable (Modifier) -> Unit,
    modifier: Modifier,
) {
    if (!window.isFullscreen) {
        PlayerOverlay(
            source = source,
            player = player,
            window = window,
            paused = paused,
            failed = failed,
            onClose = onClose,
            dispatchAction = dispatchAction,
            surface = surface,
            modifier = modifier,
        )
    } else {
        // Android keeps its full-screen layout until checkpoint 3; the video fills the screen behind it.
        surface(Modifier.fillMaxSize())
        val activity = LocalContext.current.playerActivity()
        val externalError = stringResource(resource = Res.string.player_external_error)
        var externalSourceKey by rememberSaveable { mutableStateOf<String?>(value = null) }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val sourceKey = externalSourceKey
            externalSourceKey = null
            val positionMs = mpvExPositionMillis(data = result.data)
            val active = player
            if (result.resultCode == Activity.RESULT_OK && sourceKey == source.key &&
                positionMs != null && active != null
            ) {
                dispatchAction {
                    val durationMs = active.mediaProperties.value?.durationMillis
                    active.pause()
                    active.seekTo(
                        positionMillis = if (durationMs != null && durationMs > 0L) {
                            positionMs.coerceAtMost(maximumValue = durationMs)
                        } else positionMs,
                    )
                }
            }
        }
        AndroidPlayerOverlay(
            title = source.title,
            player = player,
            window = window,
            paused = paused,
            failed = failed,
            onClose = onClose,
            onOpenExternalPlayer = {
                dispatchAction {
                    val positionMs = player?.currentPositionMillis?.value ?: source.positionMs
                    player?.pause()
                    externalSourceKey = source.key
                    val opened = openExternalPlayer(
                        source = source,
                        positionMs = positionMs,
                        launch = { launcher.launch(it) },
                    )
                    if (!opened) {
                        externalSourceKey = null
                        Toast.makeText(activity, externalError, Toast.LENGTH_LONG).show()
                    }
                }
            },
            dispatchAction = dispatchAction,
            modifier = modifier,
        )
    }
}
