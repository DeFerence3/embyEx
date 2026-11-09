package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class DetailsUi(
    val id: String,
    val title: String,
    val logoUrl: String?,
    val backdropUrl: String?,
    val episode: EpisodeContextUi?,
    val year: String?,
    val runtime: String?,
    val officialRating: String?,
    val communityRating: String?,
    val videoLabel: String?,
    val genres: List<String>,
    val overview: String?,
    val playback: PlaybackUi,
    val isFavorite: Boolean,
    val isPlayed: Boolean,
    val audioTracks: List<TrackUi>,
    val subtitleTracks: List<TrackUi>,
    val people: List<PersonUi>,
    val facts: List<MediaFactUi>,
    val isAvailableOffline: Boolean,
)
