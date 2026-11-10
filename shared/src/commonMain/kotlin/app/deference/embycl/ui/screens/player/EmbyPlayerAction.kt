package app.deference.embycl.ui.screens.player

import app.deference.embycl.domain.model.EmbyPlaybackRequest
import app.deference.embycl.domain.model.PlaybackPosition

internal sealed interface EmbyPlayerAction {
    data object Start : EmbyPlayerAction
    data object Retry : EmbyPlayerAction
    data object RetrySave : EmbyPlayerAction
    data object DismissSaveError : EmbyPlayerAction
    data class Update(
        val request: EmbyPlaybackRequest,
        val position: PlaybackPosition,
        val finished: Boolean = false,
        /** With [finished]: the queue entry to play next. Null closes the player. */
        val nextItemId: String? = null,
    ) : EmbyPlayerAction
}
