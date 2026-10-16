package app.deference.embycl.domain.model.download

import app.deference.embycl.domain.model.EmbyUserData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class PlaybackSyncBlock { CONFLICT, ITEM_UNAVAILABLE, UNSUPPORTED }

/** Durable intent is written before POST. Retries keep its watch state, refreshing unrelated server flags. */
@Serializable
data class PlaybackSyncAttempt(val revision: Long, val userData: JsonObject)

@Serializable
data class PlaybackSyncJournal(
    val block: PlaybackSyncBlock? = null,
    val serverState: EmbyUserData? = null,
    val attempt: PlaybackSyncAttempt? = null,
)

fun EmbyUserData.sameWatchState(other: EmbyUserData): Boolean =
    playbackPositionTicks == other.playbackPositionTicks && played == other.played &&
        playCount == other.playCount && lastPlayedDate == other.lastPlayedDate

enum class PlaybackSyncChoice { KEEP_LOCAL, USE_SERVER, RETRY }
