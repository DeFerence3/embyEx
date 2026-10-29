package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

// A UI slot, not the playback owner. The source, player and progress reporter stay
// in their original session composition when this slot moves to/from PiP.
private val LocalPlayerInlay = compositionLocalOf<androidx.compose.runtime.MutableState<(@Composable () -> Unit)?>> {
    error(message = "PlayerInlayHost is missing.")
}

@Composable
fun PlayerInlayHost(
    content: @Composable () -> Unit,
) {
    val slot = remember { mutableStateOf<(@Composable () -> Unit)?>(value = null) }
    CompositionLocalProvider(LocalPlayerInlay provides slot) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            slot.value?.invoke()
        }
    }
}

@Composable
internal fun PlayerInlay(
    content: @Composable () -> Unit,
) {
    val slot = LocalPlayerInlay.current
    val current by rememberUpdatedState(newValue = content)
    DisposableEffect(key1 = slot) {
        val layer: @Composable () -> Unit = { current() }
        slot.value = layer
        onDispose { if (slot.value === layer) slot.value = null }
    }
}
