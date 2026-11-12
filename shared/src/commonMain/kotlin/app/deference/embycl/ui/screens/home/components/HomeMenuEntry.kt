package app.deference.embycl.ui.screens.home.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class HomeMenuEntry(
    val icon: ImageVector,
    val label: String,
    val disabledReason: String?,
    val onClick: () -> Unit,
)
