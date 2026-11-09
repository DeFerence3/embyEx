package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class TrackUi(
    val index: Int,
    val name: String,
    val detail: String?,
    val tag: TrackTag?,
    val isDefault: Boolean,
)
