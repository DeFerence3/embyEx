package app.deference.embycl.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.repository.DownloadsRepo
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.domain.repository.OfflineUiSession
import app.deference.embycl.ui.screens.details.model.DetailsNotice
import app.deference.embycl.ui.screens.details.model.DetailsUi
import app.deference.embycl.ui.screens.details.model.DownloadStatusUi
import app.deference.embycl.ui.screens.details.model.PlaybackUi
import app.deference.embycl.ui.screens.downloads.DownloadChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class EmbyDetailsVM(
	@InjectedParam private val id: String,
	private val repository: EmbyRepository,
	private val downloads: DownloadsRepo,
	private val session: OfflineUiSession,
) : ViewModel() {

	private val _state = MutableStateFlow(value = EmbyDetailsState())
	val state = _state.asStateFlow()
	private var loading: Job? = null
	private var item: EmbyItem? = null
	private var favoriteJob: Job? = null
	private var watchedJob: Job? = null

	init {
		viewModelScope.launch {
			session.connected.collect { connected ->
				_state.update { current ->
					current.copy(
						isOffline = !connected,
						download = current.content?.getOrNull()?.downloadStatus(offline = !connected) ?: current.download,
					)
				}
			}
		}
	}

	fun onAction(action: EmbyDetailsAction) {
		when (action) {
			EmbyDetailsAction.Retry -> load()
			EmbyDetailsAction.Download -> requestDownload()
			is EmbyDetailsAction.SelectSource -> _state.update { s ->
				s.choice?.takeIf { c -> c.sources.any { it.key == action.key } }?.let { s.copy(choice = it.copy(selected = action.key)) } ?: s
			}
			EmbyDetailsAction.ConfirmDownload -> confirmDownload()
			EmbyDetailsAction.DismissDownload -> if (_state.value.choice?.submitting != true) _state.update { it.copy(choice = null) }
			EmbyDetailsAction.RequestRemoveDownload -> _state.update { it.copy(confirmRemove = true) }
			EmbyDetailsAction.DismissRemoveDownload -> _state.update { it.copy(confirmRemove = false) }
			EmbyDetailsAction.ConfirmRemoveDownload -> removeDownload()
			EmbyDetailsAction.ToggleFavorite -> toggleFavorite()
			EmbyDetailsAction.ToggleWatched -> setWatched(
				watched = _state.value.content?.getOrNull()?.isPlayed != true,
				announce = true,
			)
			EmbyDetailsAction.UndoWatched -> setWatched(
				watched = false,
				announce = false,
			)
			is EmbyDetailsAction.SelectAudio -> _state.update { it.copy(selectedAudioIndex = action.index) }
			is EmbyDetailsAction.SelectSubtitle -> _state.update { it.copy(selectedSubtitleIndex = action.index) }
			EmbyDetailsAction.DismissNotice -> _state.update { it.copy(notice = null) }
		}
	}

	private fun DetailsUi.downloadStatus(offline: Boolean): DownloadStatusUi = when {
		isAvailableOffline -> DownloadStatusUi.Downloaded
		offline -> DownloadStatusUi.Unavailable
		else -> DownloadStatusUi.Idle
	}

	private fun updateDetails(transform: (DetailsUi) -> DetailsUi) {
		_state.update { current ->
			val details = current.content?.getOrNull() ?: return@update current
			current.copy(content = Result.success(value = transform(details)))
		}
	}

	private fun toggleFavorite() {
		val details = _state.value.content?.getOrNull() ?: return
		if (favoriteJob?.isActive == true || _state.value.isOffline) return
		val target = !details.isFavorite
		updateDetails { it.copy(isFavorite = target) }
		favoriteJob = viewModelScope.launch {
			try {
				repository.toggleFavorite(
					itemId = id,
					isFavorite = target,
				)
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				updateDetails { it.copy(isFavorite = !target) }
				_state.update { it.copy(notice = DetailsNotice.FavoriteFailed) }
			}
		}
	}

	private fun setWatched(
		watched: Boolean,
		announce: Boolean,
	) {
		val before = _state.value.content?.getOrNull() ?: return
		if (watchedJob?.isActive == true || _state.value.isOffline) return
		updateDetails {
			it.copy(
				isPlayed = watched,
				playback = if (watched) PlaybackUi.Again else PlaybackUi.Start,
			)
		}
		watchedJob = viewModelScope.launch {
			try {
				repository.togglePlayed(
					itemId = id,
					isPlayed = watched,
				)
				if (announce && watched) _state.update { it.copy(notice = DetailsNotice.MarkedWatched) }
				if (!watched) refresh()
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				updateDetails {
					it.copy(
						isPlayed = before.isPlayed,
						playback = before.playback,
					)
				}
				_state.update { it.copy(notice = DetailsNotice.WatchedFailed) }
			}
		}
	}

	private fun requestDownload() {
		val current = _state.value
		val item = item ?: return
		if (current.download != DownloadStatusUi.Idle && current.download != DownloadStatusUi.Failed) return
		_state.update { it.copy(download = DownloadStatusUi.Checking, choice = null) }
		viewModelScope.launch {
			try {
				when (val request = downloads.download(item = item)) {
					is DownloadRequest.Choose -> _state.update {
						it.copy(choice = DownloadChoice(request.title, request.sources, request.selected))
					}
					is DownloadRequest.Failed -> _state.update { it.copy(notice = DetailsNotice.Message(text = request.message)) }
				}
			} catch (exception: CancellationException) {
				throw exception
			} catch (exception: Exception) {
				_state.update { it.copy(notice = DetailsNotice.Message(text = exception.message.orEmpty())) }
			} finally {
				_state.update { s -> if (s.download == DownloadStatusUi.Checking) s.copy(download = DownloadStatusUi.Idle) else s }
			}
		}
	}

	private fun confirmDownload() {
		val choice = _state.value.choice ?: return
		val key = choice.selected ?: return
		if (choice.submitting) return
		_state.update { it.copy(choice = choice.copy(submitting = true)) }
		viewModelScope.launch {
			try {
				val result = downloads.confirm(key = key)
				_state.update { s ->
					if (result.ok) {
						s.copy(
							choice = null,
							download = DownloadStatusUi.Queued,
							notice = result.message?.let { DetailsNotice.Message(text = it) } ?: DetailsNotice.DownloadQueued,
						)
					} else {
						s.copy(notice = result.message?.let { DetailsNotice.Message(text = it) } ?: s.notice)
					}
				}
			} finally {
				_state.update { it.copy(choice = it.choice?.copy(submitting = false)) }
			}
		}
	}

	private fun removeDownload() {
		_state.update { it.copy(confirmRemove = false) }
		viewModelScope.launch {
			val result = downloads.remove(itemId = id)
			result.message?.let { message -> _state.update { it.copy(notice = DetailsNotice.Message(text = message)) } }
			refresh()
		}
	}

	private fun load() {
		if (loading?.isActive == true) return
		if (_state.value.content?.isSuccess != true) _state.update { it.copy(content = null) }
		loading = viewModelScope.launch { refresh() }
	}

	private suspend fun refresh() {
		val result = try {
			val loaded = repository.item(id = id)
			item = loaded
			Result.success(value = loaded.toDetailsUi())
		} catch (exception: CancellationException) {
			throw exception
		} catch (exception: Exception) {
			Result.failure(exception = exception)
		}
		_state.update { current ->
			val details = result.getOrNull()
			when {
				details != null -> current.copy(
					content = result,
					download = when (current.download) {
						DownloadStatusUi.Queued,
						is DownloadStatusUi.Downloading,
						is DownloadStatusUi.Paused,
						DownloadStatusUi.Checking -> if (details.isAvailableOffline) DownloadStatusUi.Downloaded else current.download
						else -> details.downloadStatus(offline = current.isOffline)
					},
					selectedAudioIndex = current.selectedAudioIndex ?: details.audioTracks.firstOrNull { it.isDefault }?.index,
					selectedSubtitleIndex = if (current.content == null) {
						details.subtitleTracks.firstOrNull { it.isDefault }?.index
					} else {
						current.selectedSubtitleIndex
					},
				)
				current.content?.isSuccess != true -> current.copy(content = result)
				else -> current
			}
		}
	}
}
