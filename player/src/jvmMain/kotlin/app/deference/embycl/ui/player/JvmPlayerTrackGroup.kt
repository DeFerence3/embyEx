package app.deference.embycl.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.openani.mediamp.metadata.TrackGroup

/** MediaMP track objects stay on JVM; shared controls use only app-owned values. */
internal class JvmPlayerTrackGroup<T>(
    private val delegate: TrackGroup<T>,
    scope: CoroutineScope,
    private val id: (T) -> String,
    private val toPlayerTrack: (T) -> PlayerTrack,
) : PlayerTrackGroup {
    private val nativeCandidates = delegate.candidates.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )
    override val candidates = nativeCandidates.map { tracks -> tracks.map(transform = toPlayerTrack) }
    override val selected = delegate.selected.map { it?.let(block = toPlayerTrack) }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = delegate.selected.value?.let(block = toPlayerTrack),
    )

    override fun select(
        track: PlayerTrack?,
    ): Boolean {
        if (track == null) return delegate.select(track = null)
        val native = nativeCandidates.value.firstOrNull { id(it) == track.id } ?: return false
        return delegate.select(track = native)
    }
}
