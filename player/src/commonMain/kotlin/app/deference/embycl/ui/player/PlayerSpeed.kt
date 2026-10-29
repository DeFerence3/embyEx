package app.deference.embycl.ui.player

import kotlinx.coroutines.flow.Flow

internal interface PlayerSpeed {
    val valueFlow: Flow<Float>
    val value: Float

    fun set(
        speed: Float,
    )
}
