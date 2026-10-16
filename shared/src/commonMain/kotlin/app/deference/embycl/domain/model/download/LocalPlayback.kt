package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.repository.OfflineFile

/** A single checked private file. Deliberately separate from authenticated remote playback. */
data class LocalPlaybackRequest(
    val sessionId: String,
    val key: DownloadKey,
    val downloadId: String,
    val title: String,
    val media: OfflineFile,
    val subtitles: List<LocalPlaybackSubtitle>,
    val positionMs: Long,
)

data class LocalPlaybackSubtitle(val file: OfflineFile, val selected: Boolean)
enum class LocalPlayerError { NOT_INSTALLED, FAILED, POSITION_UNSUPPORTED, UNSUPPORTED_PLATFORM }
data class LocalPlaybackResult(
    val positionMs: Long? = null,
    val durationMs: Long? = null,
    val completed: Boolean = false,
    val error: LocalPlayerError? = null,
)
enum class LocalPlaybackOutcome { SAVED, NO_PROGRESS, CONFLICT, UNAVAILABLE, BUSY, STORAGE_FAILED, PLAYER_MISSING, PLAYER_FAILED, POSITION_UNSUPPORTED, UNSUPPORTED }

internal data class LocalPlaybackProgress(val positionTicks: Long, val played: Boolean)
internal fun localPlaybackProgress(result: LocalPlaybackResult, previouslyPlayed: Boolean): LocalPlaybackProgress? {
    if (result.error != null) return null
    val position = result.positionMs?.takeIf { it in 0..Long.MAX_VALUE / 10_000 } ?: return null
    val duration = result.durationMs?.takeIf { it in 1..Long.MAX_VALUE / 10_000 }
    if (duration != null && position > duration + 1000) return null
    val completed = result.completed || (duration != null && position >= duration)
    return LocalPlaybackProgress(if (completed) 0 else position * 10_000, completed || previouslyPlayed)
}
