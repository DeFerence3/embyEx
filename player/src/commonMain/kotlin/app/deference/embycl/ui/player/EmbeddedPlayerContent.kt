package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.deference.embycl.ui.theme.PlayerColors
import app.deference.embycl.domain.model.PlaybackPosition
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_error
import embyex.player.generated.resources.player_retry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.PlaybackEvent
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.UriMediaData

@Composable
internal fun EmbeddedPlayerContent(
    source: PlayerSource,
    window: PlayerWindowState,
    onProgress: (PlaybackPosition) -> Unit,
    onClose: (PlaybackPosition) -> Unit,
) {
    val progressCallback by rememberUpdatedState(newValue = onProgress)
    val closeCallback by rememberUpdatedState(newValue = onClose)
    var position by remember(key1 = source.key) { mutableStateOf(value = PlaybackPosition()) }
    var closed by remember(key1 = source.key) { mutableStateOf(value = false) }
    var player by remember(key1 = source.key) { mutableStateOf<MediampPlayer?>(value = null) }
    var failed by remember(key1 = source.key) { mutableStateOf(value = false) }
    var attempt by remember(key1 = source.key) { mutableStateOf(value = 0) }
    var resumePosition by rememberSaveable(source.key) { mutableStateOf(value = source.positionMs) }
    var resumePaused by rememberSaveable(source.key) { mutableStateOf(value = false) }
    val ownerScope = rememberCoroutineScope()
    val context = playerContext()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun finish() {
        if (!closed) {
            closed = true
            player?.takeIf { it.state.value.mediaStatus == MediaStatus.Ready }?.let { active ->
                position = position.copy(
                    positionMs = active.currentPositionMillis.value,
                    durationMs = active.mediaProperties.value?.durationMillis,
                )
            }
            player?.pause()
            player?.close()
            closeCallback(position.copy(paused = true))
        }
    }

    DisposableEffect(key1 = source.key) {
        onDispose {
            if (!isRecreatingPlayerHost(context = context)) finish()
        }
    }
    DisposableEffect(key1 = lifecycle, key2 = player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && window.pauseOnHostStop) {
                player?.pause()
                if (position.positionMs != null && !closed) progressCallback(position.copy(paused = true))
            }
        }
        lifecycle.addObserver(observer = observer)
        onDispose { lifecycle.removeObserver(observer = observer) }
    }

    LaunchedEffect(key1 = window.closeRequested) {
        if (window.closeRequested) finish()
    }

    PlayerPresentation(title = source.title, window = window) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = PlayerColors.background,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                player?.let { active ->
                    EmbeddedPlayerSurface(player = active, modifier = Modifier.fillMaxSize())
                }
                PlatformPlayerOverlay(
                    title = source.title,
                    player = player,
                    window = window,
                    paused = position.paused,
                    failed = failed || closed,
                    onClose = { ownerScope.launch { finish() } },
                    dispatchAction = { action -> ownerScope.launch { action() } },
                    modifier = Modifier.fillMaxSize(),
                )
                if (failed) {
                    Surface(shape = MaterialTheme.shapes.large) {
                        Column(
                            modifier = Modifier.padding(all = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                        ) {
                            Text(text = stringResource(resource = Res.string.player_error))
                            Button(onClick = { ownerScope.launch { attempt++ } }) {
                                Text(text = stringResource(resource = Res.string.player_retry))
                            }
                        }
                    }
                } else if (player == null) {
                    CircularProgressIndicator()
                }
            }
        }

    }

    // Create on the player window's UI thread and subscribe before opening media.
    LaunchedEffect(key1 = source.key, key2 = attempt) {
        failed = false
        val active = try {
            createEmbeddedPlayer(context = context, coroutineContext = coroutineContext)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            failed = true
            return@LaunchedEffect
        }
        player = active
        try {
            coroutineScope {
                var opened = false
                launch(start = CoroutineStart.UNDISPATCHED) {
                    active.events.collect { event ->
                        when (event) {
                            is PlaybackEvent.MediaEnded -> {
                                position = PlaybackPosition(
                                    positionMs = event.durationMillis ?: event.finalPositionMillis,
                                    durationMs = event.durationMillis,
                                    paused = true,
                                    completed = true,
                                )
                                finish()
                            }
                            is PlaybackEvent.SeekCompleted -> if (opened && !closed) {
                                position = position.copy(positionMs = event.positionMillis)
                                progressCallback(position)
                            }
                            is PlaybackEvent.ErrorOccurred -> failed = true
                            is PlaybackEvent.ExternalPlayWhenReadyChanged -> Unit
                        }
                    }
                }
                launch {
                    var previousPaused: Boolean? = null
                    combine(active.state, active.currentPositionMillis, active.mediaProperties) { state, time, metadata ->
                        if (state.mediaStatus == MediaStatus.Ready) {
                            PlaybackPosition(
                                positionMs = time,
                                durationMs = metadata?.durationMillis,
                                paused = !state.playWhenReady,
                            )
                        } else null
                    }.collect { latest ->
                        if (opened && latest != null && !closed) {
                            position = latest
                            resumePosition = latest.positionMs ?: resumePosition
                            resumePaused = latest.paused
                            if (previousPaused != latest.paused) progressCallback(latest)
                            previousPaused = latest.paused
                        }
                    }
                }
                active.setMediaData(
                    data = UriMediaData(
                        uri = source.uri,
                        headers = source.headers,
                        extraFiles = MediaExtraFiles(
                            subtitles = source.subtitles,
                        ),
                    ),
                    playWhenReady = !resumePaused && lifecycle.currentState.isAtLeast(state = Lifecycle.State.STARTED),
                    startPositionMillis = resumePosition,
                )
                opened = true
                if (!closed) {
                    position = PlaybackPosition(
                        positionMs = active.currentPositionMillis.value,
                        durationMs = active.mediaProperties.value?.durationMillis,
                        paused = !active.state.value.playWhenReady,
                    )
                    progressCallback(position)
                }
                launch {
                    while (true) {
                        delay(timeMillis = 10_000L)
                        if (!closed && !failed && position.positionMs != null) progressCallback(position)
                    }
                }
                awaitCancellation()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Backend exceptions can contain credentials; keep UI and logs sanitized.
            failed = true
        } finally {
            active.close()
            player = null
        }
    }
}
