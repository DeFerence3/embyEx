package app.deference.embycl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.deference.embycl.ui.theme.DarkColorScheme
import app.deference.embycl.ui.theme.LightColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class ExpressiveContrastTest {
    @Test
    fun semanticTextColorsMeetNormalTextContrast() {
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (mode, scheme) ->
            with(scheme) {
                val pairs = listOf(
                    "primary" to (onPrimary to primary),
                    "primaryContainer" to (onPrimaryContainer to primaryContainer),
                    "secondary" to (onSecondary to secondary),
                    "secondaryContainer" to (onSecondaryContainer to secondaryContainer),
                    "tertiary" to (onTertiary to tertiary),
                    "tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
                    "surface" to (onSurface to surface),
                    "surfaceContainerLow" to (onSurface to surfaceContainerLow),
                    "surfaceContainerHigh" to (onSurfaceVariant to surfaceContainerHigh),
                    "subtle text" to (onSurfaceVariant to surface),
                    "accent text" to (primary to surface),
                    "error" to (onErrorContainer to errorContainer),
                )
                pairs.forEach { (role, colors) ->
                    val ratio = contrast(colors.first, colors.second)
                    assertTrue(ratio >= 4.5f, "$mode $role contrast was $ratio; normal text needs 4.5:1")
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float =
        (max(a.luminance(), b.luminance()) + .05f) / (min(a.luminance(), b.luminance()) + .05f)
}
