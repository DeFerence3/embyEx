package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal interface PlayerTrackGroup {
    val candidates: Flow<List<PlayerTrack>>
    val selected: StateFlow<PlayerTrack?>

    /** Null disables the track; false means the backend rejected the selection. */
    fun select(
        track: PlayerTrack?,
    ): Boolean
}
