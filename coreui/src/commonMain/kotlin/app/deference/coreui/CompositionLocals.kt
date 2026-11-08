package app.deference.coreui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import app.deference.coreui.keyboard.KeyShortcutManager

val LocalKeyShortcutManager: ProvidableCompositionLocal<KeyShortcutManager> =
	compositionLocalOf { error("No KeyShortcutManager provided") }