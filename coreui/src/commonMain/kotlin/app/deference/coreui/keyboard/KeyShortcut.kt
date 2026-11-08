package app.deference.coreui.keyboard

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

data class KeyShortcut(
    val key: Key,
    val description: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
    val onAction: () -> Unit
) {
    fun matches(event: KeyEvent): Boolean {
        return event.key == key &&
                event.isCtrlPressed == ctrl &&
                event.isShiftPressed == shift &&
                event.isAltPressed == alt &&
                event.isMetaPressed == meta
    }
}
