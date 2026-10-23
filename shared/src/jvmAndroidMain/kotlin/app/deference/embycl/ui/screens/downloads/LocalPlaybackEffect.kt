package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import app.deference.embycl.domain.model.PlaybackPosition
import app.deference.embycl.domain.model.download.LocalPlaybackRequest
import app.deference.embycl.domain.model.download.LocalPlaybackResult
import app.deference.embycl.ui.player.EmbeddedPlayerDialog
import app.deference.embycl.ui.player.PlayerSource
import org.openani.mediamp.source.Subtitle
import java.io.File

actual val localPlaybackSupported: Boolean = true

@Composable
actual fun LocalPlaybackEffect(
    request: LocalPlaybackRequest?,
    onResult: (String, LocalPlaybackResult) -> Unit,
    onProgress: (String, LocalPlaybackResult) -> Unit,
) {
    if (request == null) return
    key(request.sessionId) {
        EmbeddedPlayerDialog(
            source = PlayerSource(
                key = request.sessionId,
                title = request.title,
                uri = File(request.media.absolutePath).toURI().toASCIIString(),
                positionMs = request.positionMs,
                subtitles = request.subtitles.map { subtitle ->
                    val file = File(subtitle.file.absolutePath)
                    Subtitle(
                        uri = file.toURI().toASCIIString(),
                        mimeType = when (file.extension.lowercase()) {
                            "srt" -> "application/x-subrip"
                            "vtt" -> "text/vtt"
                            "ass", "ssa" -> "text/x-ssa"
                            else -> null
                        },
                        label = file.nameWithoutExtension,
                    )
                },
            ),
            onProgress = { position -> onProgress(request.sessionId, position.localResult()) },
            onClose = { position -> onResult(request.sessionId, position.localResult()) },
        )
    }
}

private fun PlaybackPosition.localResult(): LocalPlaybackResult = LocalPlaybackResult(
    positionMs = positionMs,
    durationMs = durationMs,
    completed = completed,
)
