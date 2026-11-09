package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class MediaFactUi(
    val label: StringResource,
    val value: String?,
    val trackCount: Int? = null,
)
