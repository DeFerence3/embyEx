package app.deference.embycl.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/** Window actions never replace the player, its composition, or its media source. */
internal class PlayerWindowState(
    val supportsPictureInPicture: Boolean = false,
    val supportsFullscreen: Boolean = false,
    val usesSystemPictureInPicture: Boolean = false,
    val togglePictureInPicture: () -> Unit = {},
    val toggleFullscreen: () -> Unit = {},
    var startMove: () -> Unit = {},
    val moveBy: (Float, Float) -> Unit = { _, _ -> },
    val pauseOnHostStop: Boolean = true,
) {
    var isPictureInPicture by mutableStateOf(value = false)
    var isFullscreen by mutableStateOf(value = false)
    var closeRequested by mutableStateOf(value = false)
    var pictureInPictureFailed by mutableStateOf(value = false)
    // Last laid-out video rectangle in window pixels, used as Android's PiP source hint.
    var videoBounds: Rect? = null
}
