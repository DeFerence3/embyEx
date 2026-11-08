package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.key.Key
import app.deference.coreui.keyboard.KeyShortcut
import app.deference.coreui.keyboard.RegisterShortcuts

/**
 * Registers Space and the media Play/Pause key to toggle [player] playback.
 *
 * A registered shortcut always consumes its key, so the shortcuts are registered only while
 * [enabled] is true and [player] exists. Callers disable them whenever Space belongs to
 * something else, such as a focused control, an open menu or sheet, or locked controls.
 *
 * @param onTriggered Runs on every toggle before playback changes, e.g. to reveal controls.
 */
@Composable
internal fun RegisterPlaybackShortcuts(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onTriggered: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
) {
    val currentOnTriggered by rememberUpdatedState(newValue = onTriggered)
    val currentDispatchAction by rememberUpdatedState(newValue = dispatchAction)
    val shortcuts = remember(enabled, player) {
        val active = player
        if (!enabled || active == null) {
            emptyList()
        } else {
            val togglePlayback: () -> Unit = {
                currentOnTriggered()
                currentDispatchAction { active.togglePlayWhenReady() }
            }
            listOf(
                KeyShortcut(
                    key = Key.Spacebar,
                    description = "Play Pause",
                    onAction = togglePlayback,
                ),
                KeyShortcut(
                    key = Key.MediaPlayPause,
                    description = "Play Pause",
                    onAction = togglePlayback,
                ),
            )
        }
    }
    RegisterShortcuts(
        shortcuts = shortcuts,
        key = "Player Playback Shortcuts",
    )
}
