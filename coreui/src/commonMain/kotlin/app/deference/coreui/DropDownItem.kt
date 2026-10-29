package app.deference.coreui

import androidx.compose.ui.graphics.vector.ImageVector

data class DropDownItem(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
