package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SeasonUi {
    data object Hidden : SeasonUi

    data object Loading : SeasonUi

    data class Loaded(
        val seasonNumber: Int,
        val episodes: List<SeasonEpisodeUi>,
        val previous: EpisodeLinkUi?,
        val next: EpisodeLinkUi?,
    ) : SeasonUi

    data class Failed(val seasonNumber: Int) : SeasonUi
}
