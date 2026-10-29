package app.deference.embycl.ui.screens.player

import app.deference.embycl.domain.model.EmbyPlaybackRequest

internal data class EmbyPlayerState(
    val request: EmbyPlaybackRequest? = null,
    val preparing: Boolean = true,
    val failed: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val finished: Boolean = false,
)
