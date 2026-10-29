package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps native track IDs and the existing audio/subtitle controls in sync. */
internal class AndroidMpvTrackGroup(
    private val trackId: (PlayerTrack) -> String,
    private val selectNative: (String) -> Boolean,
) : PlayerTrackGroup {
    private val mutableCandidates = MutableStateFlow<List<PlayerTrack>>(value = emptyList())
    private val mutableSelected = MutableStateFlow<PlayerTrack?>(value = null)
    override val candidates = mutableCandidates.asStateFlow()
    override val selected = mutableSelected.asStateFlow()

    override fun select(
        track: PlayerTrack?,
    ): Boolean {
        if (track != null && track !in mutableCandidates.value) return false
        if (!selectNative(track?.let { trackId(it) } ?: "no")) return false
        mutableSelected.value = track
        return true
    }

    fun refresh(
        tracks: List<PlayerTrack>,
        selectedId: String?,
    ) {
        mutableCandidates.value = tracks
        mutableSelected.value = tracks.firstOrNull { trackId(it) == selectedId }
    }

    fun clear() {
        mutableCandidates.value = emptyList()
        mutableSelected.value = null
    }
}
