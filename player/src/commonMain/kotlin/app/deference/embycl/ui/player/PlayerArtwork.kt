package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Draws an artwork image for a queue entry. Hosts provide their authenticated image loader; without one the
 * player shows a plain tile. The image is decorative: the entry's text says what it is.
 */
val LocalPlayerArtwork: ProvidableCompositionLocal<(@Composable (uri: String, modifier: Modifier) -> Unit)?> =
    compositionLocalOf { null }
