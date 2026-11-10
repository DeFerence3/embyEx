package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Player-owned volume, separate from the system volume. [volume] is a fraction from 0 to 1, where 1 is
 * the source's original level; values outside that range are clamped by [setVolume].
 */
internal interface PlayerAudioLevel {
    val volume: StateFlow<Float>
    val muted: StateFlow<Boolean>

    fun setVolume(
        volume: Float,
    )

    fun setMuted(
        muted: Boolean,
    )
}
