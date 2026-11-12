package app.deference.embycl.ui.screens.home.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.cardMenuTriggers(onOpenMenu: () -> Unit): Modifier = composed {
    val open by rememberUpdatedState(newValue = onOpenMenu)
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consume() }
                        open()
                    }
                }
            }
        }
        .onPreviewKeyEvent { event ->
            val menuKey = event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed)
            if (menuKey && event.type == KeyEventType.KeyDown) {
                open()
                true
            } else {
                false
            }
        }
}
