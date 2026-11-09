package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class SeasonEpisodeUi(
    val id: String,
    val title: String,
    val episodeNumber: Int?,
    val runtime: String?,
    val stillUrl: String?,
    val isPlayed: Boolean,
    val progress: Float?,
    val isCurrent: Boolean,
)
