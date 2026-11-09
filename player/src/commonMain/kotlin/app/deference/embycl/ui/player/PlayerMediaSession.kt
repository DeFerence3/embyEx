package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import app.deference.embycl.core.utils.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.TimeSource

/** A position change larger than playback could explain is a seek. */
private const val SEEK_JUMP_MILLIS = 1_500L

/** Scrubbing seeks once per drag step; the system is told once the position settles. */
private const val SEEK_SETTLE_MILLIS = 200L

private const val LOG_TAG = "PlayerMediaSession"

/**
 * Publishes [player] to the system media controls while it is in the composition.
 *
 * Only transport changes and seeks are published, not position ticks: the system extrapolates the
 * position while playing.
 *
 * @param dispatchAction Runs a system command on the player's dispatcher.
 * @param onStop Asks the host to close the player.
 */
@Composable
internal fun RegisterPlayerMediaSession(
    source: PlayerSource,
    player: EmbeddedPlayer?,
    dispatchAction: (() -> Unit) -> Unit,
    onStop: () -> Unit,
) {
    val context = playerContext()
    val currentDispatchAction by rememberUpdatedState(newValue = dispatchAction)
    val currentOnStop by rememberUpdatedState(newValue = onStop)
    var session by remember(key1 = player) { mutableStateOf<PlatformMediaSession?>(value = null) }

    DisposableEffect(key1 = player) {
        val created = player?.let { active ->
            try {
                createPlatformMediaSession(
                    context = context,
                    source = source,
                    player = active,
                    dispatchAction = { action -> currentDispatchAction(action) },
                    onStop = { currentOnStop() },
                )
            } catch (exception: Exception) {
                // Optional feature; the message may contain paths or addresses, so only the type is logged.
                Log.w(tag = LOG_TAG) { "Media controls unavailable: ${exception::class.simpleName}" }
                null
            }
        }
        session = created
        onDispose {
            session = null
            created?.release()
        }
    }

    val active = session
    if (active != null && player != null) {
        LaunchedEffect(key1 = active, key2 = source.title) {
            publishTransport(
                session = active,
                title = source.title,
                player = player,
            )
        }
    }
}

private suspend fun publishTransport(
    session: PlatformMediaSession,
    title: String,
    player: EmbeddedPlayer,
) = coroutineScope {
    val jumps = Channel<Unit>(capacity = Channel.CONFLATED)
    launch {
        combine(player.state, player.mediaProperties, player.playbackSpeed?.valueFlow ?: flowOf(value = 1f)) { _, _, _ -> }
            .collect { session.update(snapshot = player.mediaSessionSnapshot(title = title), seeked = false) }
    }
    launch { player.detectSeeks(jumps = jumps) }
    launch {
        jumps.receiveAsFlow().collectLatest {
            delay(timeMillis = SEEK_SETTLE_MILLIS)
            session.update(snapshot = player.mediaSessionSnapshot(title = title), seeked = true)
        }
    }
}

/** Signals [jumps] whenever the position moves further than elapsed playback time explains. */
private suspend fun EmbeddedPlayer.detectSeeks(
    jumps: SendChannel<Unit>,
) {
    var lastPosition = currentPositionMillis.value
    var lastMark = TimeSource.Monotonic.markNow()
    currentPositionMillis.collect { position ->
        val current = state.value
        val ready = current.mediaStatus == PlayerMediaStatus.Ready
        val expected = if (ready && current.playWhenReady && !current.isBuffering) {
            lastPosition + (lastMark.elapsedNow().inWholeMilliseconds * (playbackSpeed?.value ?: 1f)).toLong()
        } else {
            lastPosition
        }
        // Positions applied while opening are the resume point, not a seek.
        if (ready && abs(position - expected) > SEEK_JUMP_MILLIS) jumps.trySend(element = Unit)
        lastPosition = position
        lastMark = TimeSource.Monotonic.markNow()
    }
}

private fun EmbeddedPlayer.mediaSessionSnapshot(
    title: String,
): MediaSessionSnapshot = MediaSessionSnapshot(
    title = title,
    status = state.value.toMediaSessionStatus(),
    positionMillis = currentPositionMillis.value,
    durationMillis = mediaProperties.value?.durationMillis,
    speed = playbackSpeed?.value ?: 1f,
)

private fun PlayerState.toMediaSessionStatus(): MediaSessionStatus = when (mediaStatus) {
    PlayerMediaStatus.Opening -> MediaSessionStatus.Buffering
    PlayerMediaStatus.Ready -> when {
        !playWhenReady -> MediaSessionStatus.Paused
        isBuffering -> MediaSessionStatus.Buffering
        else -> MediaSessionStatus.Playing
    }
    PlayerMediaStatus.Idle,
    PlayerMediaStatus.Ended,
    PlayerMediaStatus.Error,
    PlayerMediaStatus.Released,
    -> MediaSessionStatus.Stopped
}
