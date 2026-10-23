package app.deference.embycl.ui.screens.details.components

import androidx.compose.runtime.Composable
import app.deference.embycl.ui.screens.details.EmbyDetailsAction
import androidx.compose.runtime.key
import app.deference.embycl.domain.model.EmbyPlaybackRequest
import app.deference.embycl.ui.player.EmbeddedPlayerDialog
import app.deference.embycl.ui.player.PlayerSource

@Composable
internal fun PlaybackEffect(
    request: EmbyPlaybackRequest?,
    onAction: (EmbyDetailsAction) -> Unit,
) {
    if (request == null) return
    key(request.sessionId) {
        EmbeddedPlayerDialog(
            source = PlayerSource(
                key = request.sessionId,
                title = request.item.title,
                uri = request.item.url,
                positionMs = request.item.positionMs,
                headers = mapOf("X-Emby-Token" to request.accessToken),
            ),
            onProgress = { position ->
                onAction(EmbyDetailsAction.PlaybackUpdate(request = request, position = position))
            },
            onClose = { position ->
                onAction(EmbyDetailsAction.PlaybackUpdate(request = request, position = position, finished = true))
            },
        )
    }
}
