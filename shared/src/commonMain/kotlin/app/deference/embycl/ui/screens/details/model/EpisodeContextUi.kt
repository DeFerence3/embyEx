package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class EpisodeContextUi(
    val seriesId: String?,
    val seriesName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
)
