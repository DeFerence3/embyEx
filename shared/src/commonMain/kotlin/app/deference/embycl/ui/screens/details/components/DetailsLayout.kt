package app.deference.embycl.ui.screens.details.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class DetailsLayout {
    Compact,

    Medium,

    Expanded,
    ;

    companion object {
        fun fromSize(
            width: Dp,
            height: Dp,
        ): DetailsLayout = when {
            width < 600.dp -> Compact
            width >= 840.dp || height < 500.dp -> Expanded
            else -> Medium
        }

        fun scaleFor(
            width: Dp,
            height: Dp,
        ): Float = when {
            width >= 1440.dp && height >= 800.dp -> 1.25f
            width >= 1100.dp && height >= 640.dp -> 1.15f
            else -> 1f
        }
    }
}
