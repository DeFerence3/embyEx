package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class PersonUi(
    val id: String?,
    val name: String,
    val role: String?,
    val kind: PersonKind,
    val photoUrl: String?,
    val initials: String,
)
