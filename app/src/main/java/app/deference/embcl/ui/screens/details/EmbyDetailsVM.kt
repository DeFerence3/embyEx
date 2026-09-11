package app.deference.embcl.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.domain.model.EmbyPlaybackEvent
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmbyDetailsVM(
	@InjectedParam private val id: String,
	private val repository: EmbyRepository,
	sessionStore: EmbySessionStore,
) : ViewModel() {
	private val _state = MutableStateFlow(EmbyDetailsState(session = sessionStore.session.value))
	val state = _state.asStateFlow()
	private val _events = Channel<EmbyDetailsEvent>(Channel.BUFFERED)
	val events = _events.receiveAsFlow()

	init { load() }

	fun onAction(action: EmbyDetailsAction) {
		when (action) {
			EmbyDetailsAction.Retry -> load()
			EmbyDetailsAction.Play -> preparePlayback()
			is EmbyDetailsAction.PlaybackFinished -> {
				action.positionMs?.let {
					repository.reportPlayback(id, it * 10_000L, EmbyPlaybackEvent.Stopped)
				}
				load()
			}
		}
	}

	private fun preparePlayback() {
		val item = _state.value.content?.getOrNull() ?: return
		val session = _state.value.session ?: return
		viewModelScope.launch {
			val parentFolderId = item.seasonId ?: item.parentId
			val siblings = if (item.isEpisode() && !parentFolderId.isNullOrBlank()) {
				runCatching { repository.items(parentFolderId).items }.getOrDefault(listOf(item))
			} else listOf(item)
			val selectedIndex = siblings.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
			repository.reportPlayback(item.id, item.userData?.playbackPositionTicks ?: 0L, EmbyPlaybackEvent.Started)
			_events.send(
				EmbyDetailsEvent.LaunchPlayback(
					EmbyPlaybackRequest(
						itemId = item.id,
						title = item.name,
						urls = ArrayList(siblings.map(repository::streamUrl)),
						selectedIndex = selectedIndex,
						positionMs = ((item.userData?.playbackPositionTicks ?: 0L) / 10_000L).toInt(),
						accessToken = session.accessToken,
					)
				)
			)
		}
	}

	private fun load() {
		if (_state.value.session == null) return
		_state.value = _state.value.copy(content = null)
		viewModelScope.launch {
			val result = runCatching { repository.item(id) }
			_state.value = _state.value.copy(content = result)
			result.exceptionOrNull()?.let { _events.send(EmbyDetailsEvent.Error(it.message ?: "Could not load item.")) }
		}
	}
}
