package app.deference.embycl.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.domain.repository.EmbyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
internal class EmbyPlayerViewModel(
    @InjectedParam private val destination: EmbyPlayerScreen,
    private val repository: EmbyRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(value = EmbyPlayerState())
    val state = mutableState.asStateFlow()
    private val updates = Channel<EmbyPlayerAction.Update>(capacity = Channel.CONFLATED)
    private var pendingSave: EmbyPlayerAction.Update? = null
    private var reportedSession: String? = null
    private var preparation: Job? = null
    private var started = false

    init {
        viewModelScope.launch {
            for (update in updates) savePlayback(update = update)
        }
    }

    fun onAction(
        action: EmbyPlayerAction,
    ) {
        when (action) {
            EmbyPlayerAction.Start -> if (!started) {
                started = true
                prepare()
            }
            EmbyPlayerAction.Retry -> prepare()
            is EmbyPlayerAction.Update -> {
                if (state.value.request?.sessionId == action.request.sessionId && !state.value.saving) {
                    if (action.finished) mutableState.update { it.copy(saving = true) }
                    updates.trySend(element = action)
                }
            }
            EmbyPlayerAction.RetrySave -> pendingSave?.let { update ->
                if (!state.value.saving) {
                    pendingSave = null
                    mutableState.update { it.copy(saving = update.finished, saveFailed = false) }
                    updates.trySend(element = update)
                }
            }
            EmbyPlayerAction.DismissSaveError -> {
                val leaving = pendingSave?.finished == true
                pendingSave = null
                mutableState.update { it.copy(saveFailed = false, finished = leaving) }
            }
        }
    }

    private fun prepare() {
        if (preparation?.isActive == true) return
        mutableState.update { it.copy(preparing = true, failed = false) }
        preparation = viewModelScope.launch {
            try {
                val item = repository.item(id = destination.id)
                // Local playback holds its file lease until the screen reports completion.
                val request = if (destination.fromServer) repository.playFromServer(item = item)
                    else repository.play(item = item)
                mutableState.update { it.copy(request = request, preparing = false, finished = request == null) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableState.update { it.copy(preparing = false, failed = true) }
            }
        }
    }

    private suspend fun savePlayback(
        update: EmbyPlayerAction.Update,
    ) {
        val milliseconds = update.position.positionMs
        if (milliseconds == null || milliseconds !in 0..Long.MAX_VALUE / 10_000L) {
            if (update.finished) mutableState.update { it.copy(saving = false, finished = true) }
            return
        }
        try {
            if (reportedSession != update.request.sessionId) {
                repository.reportPlayback(
                    request = update.request,
                    positionTicks = milliseconds * 10_000L,
                    event = EmbyPlaybackEvent.Started,
                    isPaused = update.position.paused,
                )
                reportedSession = update.request.sessionId
            }
            repository.reportPlayback(
                request = update.request,
                positionTicks = milliseconds * 10_000L,
                event = if (update.finished) EmbyPlaybackEvent.Stopped else EmbyPlaybackEvent.Progress,
                isPaused = update.position.paused,
            )
            pendingSave = null
            mutableState.update { it.copy(saveFailed = false) }
            if (update.finished) {
                val next = update.request.selectedIndex + 1
                if (update.position.completed && next < update.request.items.size) {
                    mutableState.update {
                        it.copy(
                            saving = false,
                            request = update.request.copy(selectedIndex = next, sessionId = Uuid.random().toString()),
                        )
                    }
                } else {
                    mutableState.update { it.copy(saving = false, finished = true) }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            pendingSave = update
            mutableState.update { it.copy(saveFailed = true, saving = false) }
        }
    }
}
