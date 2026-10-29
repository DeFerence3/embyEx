package app.deference.embycl.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.deference.embycl.domain.model.PlaybackPosition
import app.deference.embycl.ui.theme.PlayerColors
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
    var closing by remember(key1 = source.key) { mutableStateOf(value = false) }
    var player by remember(key1 = source.key) { mutableStateOf<EmbeddedPlayer?>(value = null) }
    var failed by remember(key1 = source.key) { mutableStateOf(value = false) }
    var attempt by remember(key1 = source.key) { mutableStateOf(value = 0) }
    var resumePosition by rememberSaveable(source.key) { mutableStateOf(value = source.positionMs) }
    var resumePaused by rememberSaveable(source.key) { mutableStateOf(value = false) }
    val ownerScope = rememberCoroutineScope()
    val context = playerContext()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    fun requestClose() {
        if (!closing && !closed) {
            closing = true
            player?.pause()
        }
    }

    fun finish() {
        if (!closed) {
            closed = true
            player?.takeIf { !position.completed && it.state.value.mediaStatus == PlayerMediaStatus.Ready }?.let { active ->
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
        if (window.closeRequested) requestClose()
    }

    LaunchedEffect(key1 = closing) {
        if (closing) finish()
    }

    PlayerPresentation(title = source.title, window = window) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = PlayerColors.background)
                .onGloballyPositioned { window.videoBounds = it.boundsInWindow() }
                .semantics { paneTitle = source.title },
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                player?.let { active ->
                    EmbeddedPlayerSurface(player = active, modifier = Modifier.fillMaxSize())
                }
                PlatformPlayerOverlay(
                    source = source,
                    player = player,
                    window = window,
                    paused = position.paused,
                    failed = failed || closed || closing,
                    onClose = { ownerScope.launch { requestClose() } },
                    dispatchAction = { action -> ownerScope.launch { action() } },
                    modifier = Modifier.fillMaxSize(),
                )
                if (failed) {
                    Surface(shape = MaterialTheme.shapes.large) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(state = rememberScrollState())
                                .padding(all = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                        ) {
                            Text(text = stringResource(resource = Res.string.player_error))
                            Button(onClick = { ownerScope.launch { attempt++ } }) {
                                Text(text = stringResource(resource = Res.string.player_retry))
                            }
                        }
                    }
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
        if (closing || closed) {
            active.close()
            return@LaunchedEffect
        }
        player = active
        try {
            coroutineScope {
                var opened = false
                launch(start = CoroutineStart.UNDISPATCHED) {
                    active.events.collect { event ->
                        when (event) {
                            is PlayerEvent.MediaEnded -> {
                                position = PlaybackPosition(
                                    positionMs = event.durationMillis ?: event.finalPositionMillis,
                                    durationMs = event.durationMillis,
                                    paused = true,
                                    completed = true,
                                )
                                requestClose()
                            }
                            is PlayerEvent.SeekCompleted -> if (opened && !closed && !closing) {
                                position = position.copy(positionMs = event.positionMillis)
                                progressCallback(position)
                            }
                            is PlayerEvent.ErrorOccurred -> failed = true
                        }
                    }
                }
                launch {
                    var previousPaused: Boolean? = null
                    combine(active.state, active.currentPositionMillis, active.mediaProperties) { state, time, metadata ->
                        if (state.mediaStatus == PlayerMediaStatus.Ready) {
                            PlaybackPosition(
                                positionMs = time,
                                durationMs = metadata?.durationMillis,
                                paused = !state.playWhenReady,
                            )
                        } else null
                    }.collect { latest ->
                        if (opened && latest != null && !closed && !closing) {
                            position = latest
                            resumePosition = latest.positionMs ?: resumePosition
                            resumePaused = latest.paused
                            if (previousPaused != latest.paused) progressCallback(latest)
                            previousPaused = latest.paused
                        }
                    }
                }
                active.open(
                    source = source,
                    playWhenReady = !closing && !resumePaused && lifecycle.currentState.isAtLeast(state = Lifecycle.State.STARTED),
                    startPositionMillis = resumePosition,
                )
                opened = true
                if (closing) active.pause()
                if (!closed && !closing) {
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
                        if (!closed && !closing && !failed && position.positionMs != null) progressCallback(position)
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
