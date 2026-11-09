package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class EpisodeLinkUi(
    val id: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
)
