package app.deference.embycl.ui.screens.home.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeCardUi(
    val id: String,
    val title: String,
    val type: String,
    val isFolder: Boolean,
    val kind: HomeCardKind,
    val seriesId: String?,
    val seriesName: String?,
    val episodeLabel: String?,
    val year: Int?,
    val minutesLeft: Int?,
    val runtimeMinutes: Int?,
    val progress: Float?,
    val isWatched: Boolean,
    val isFavorite: Boolean,
    val isDownloaded: Boolean,
    val posterUrl: String?,
    val stillUrl: String?,
    val backdropUrl: String?,
)
