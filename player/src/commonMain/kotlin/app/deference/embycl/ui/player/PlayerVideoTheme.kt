package app.deference.embycl.ui.player

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * The colour scheme for controls drawn over video. Hosts provide their dark scheme so the controls keep the
 * same contrast in both app themes; without one, the surrounding scheme is used.
 */
val LocalPlayerVideoColorScheme: ProvidableCompositionLocal<ColorScheme?> = compositionLocalOf { null }

/** Applies [LocalPlayerVideoColorScheme] while keeping the host's shapes, type and motion. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlayerVideoTheme(
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = LocalPlayerVideoColorScheme.current ?: MaterialTheme.colorScheme,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
