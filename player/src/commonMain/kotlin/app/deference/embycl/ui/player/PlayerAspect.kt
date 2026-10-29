package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.StateFlow

internal interface PlayerAspect {
    val mode: StateFlow<PlayerAspectRatio>

    fun setMode(
        mode: PlayerAspectRatio,
    )
}
