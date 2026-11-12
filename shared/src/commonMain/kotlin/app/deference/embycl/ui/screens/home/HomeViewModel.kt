package app.deference.embycl.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.download.DownloadRequest
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionResult
import app.deference.embycl.domain.repository.DownloadsRepo
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.domain.repository.HomeLayoutRepository
import app.deference.embycl.domain.repository.OfflineUiSession
import app.deference.embycl.domain.repository.ServerReconnector
import app.deference.embycl.ui.screens.downloads.DownloadChoice
import app.deference.embycl.ui.screens.home.model.HomeFullState
import app.deference.embycl.ui.screens.home.model.HomeNotice
import app.deference.embycl.ui.screens.home.model.HomeSourceUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeViewModel(
	private val repository: EmbyRepository,
	private val reconnector: ServerReconnector,
	private val session: OfflineUiSession,
	private val layoutRepository: HomeLayoutRepository,
	private val downloads: DownloadsRepo,
) : ViewModel() {

	private val _state = MutableStateFlow(
		HomeState(
			serverName = Session.serverName,
			serverAddress = if (session.browsingLocalFiles) "" else session.address(),
		),
	)
	val state: StateFlow<HomeState> = _state.asStateFlow()
	private var layout: HomeLayoutPrefs? = null
	private var home: EmbyHome? = null
	private var loadJob: Job? = null
	private var retryJob: Job? = null
	private var watchedJob: Job? = null
	private var favoriteJob: Job? = null
	private var hideJob: Job? = null

	init {
		viewModelScope.launch { layoutRepository.layout.collect { onLayout(next = it) } }
		viewModelScope.launch { repository.changes.collect { load(refresh = true) } }
		viewModelScope.launch {
			session.connected
				.drop(count = 1)
				.distinctUntilChanged()
				.collect { load(refresh = true) }
		}
	}

	fun onAction(action: HomeAction) {
		when (action) {
			HomeAction.Refresh -> {
				reconnector.reconnectNow()
				load(refresh = true)
			}
			is HomeAction.SelectSlide -> _state.update { current ->
				val content = current.content ?: return@update current
				val spotlight = content.spotlight ?: return@update current
				current.copy(
					content = content.copy(
						spotlight = spotlight.copy(selected = action.index.coerceIn(0, spotlight.items.lastIndex)),
					),
				)
			}
			is HomeAction.RetrySection -> retrySection(key = action.key)
			is HomeAction.ToggleWatched -> {
				val card = _state.value.content?.findCard(itemId = action.itemId) ?: return
				setWatched(
					itemId = card.id,
					title = card.title,
					watched = !card.isWatched,
				)
			}
			is HomeAction.ToggleFavorite -> toggleFavorite(itemId = action.itemId)
			is HomeAction.RemoveFromContinue -> {
				val card = _state.value.content?.findCard(itemId = action.itemId) ?: return
				setHidden(
					itemId = card.id,
					title = card.title,
					hide = true,
				)
			}
			HomeAction.Undo -> undo()
			HomeAction.DismissNotice -> _state.update { it.copy(notice = null) }
			is HomeAction.RequestDownload -> requestDownload(itemId = action.itemId)
			is HomeAction.SelectSource -> _state.update { it.copy(downloadChoice = it.downloadChoice?.copy(selected = action.key)) }
			HomeAction.ConfirmDownload -> confirmDownload()
			HomeAction.DismissDownload -> _state.update { it.copy(downloadChoice = null) }
		}
	}

	private fun onLayout(next: HomeLayoutPrefs) {
		val previous = layout
		layout = next
		val loaded = home
		if (previous == null || loaded == null) {
			load(refresh = previous != null)
			return
		}
		val before = previous.resolvedFor(libraries = loaded.libraries).visibleSections.toSet()
		val after = next.resolvedFor(libraries = loaded.libraries).visibleSections.toSet()
		val missing = (after - before - setOf(HomeSectionId.Downloaded)).filterNot { it in loaded.sections.keys }
		if (missing.isNotEmpty()) {
			load(refresh = true)
		} else {
			_state.update { current -> current.remapped(home = loaded, layout = next) }
		}
	}

	private fun HomeState.remapped(
		home: EmbyHome,
		layout: HomeLayoutPrefs,
	): HomeState {
		val resolved = layout.resolvedFor(libraries = home.libraries)
		val content = home.toContentUi(
			layout = resolved,
			previous = content?.spotlight,
		)
		val empty = content.spotlight == null && content.sections.isEmpty()
		return copy(
			content = content.takeUnless { empty },
			fullState = if (empty) emptyState(local = session.browsingLocalFiles, connected = session.connected.value) else null,
			cardSize = resolved.cardSize,
		)
	}

	private fun load(refresh: Boolean) {
		val current = layout ?: return
		loadJob?.cancel()
		loadJob = viewModelScope.launch {
			_state.update { state ->
				state.copy(
					isRefreshing = refresh && state.content != null,
					source = when {
						state.content == null && state.fullState == null -> HomeSourceUi.Connecting
						refresh && !session.browsingLocalFiles && !session.connected.value -> HomeSourceUi.Reconnecting
						else -> state.source
					},
				)
			}
			val result = try {
				Result.success(value = repository.home(layout = current))
			} catch (exception: CancellationException) {
				throw exception
			} catch (exception: Exception) {
				Result.failure(exception = exception)
			}
			result.getOrNull()?.let { home = it }
			val local = session.browsingLocalFiles
			val connected = session.connected.value
			val signedOut = signedOut()
			_state.update { state ->
				val base = state.copy(
					isRefreshing = false,
					serverName = Session.serverName,
					serverAddress = if (local) "" else session.address(),
				)
				result.fold(
					onSuccess = { loaded ->
						base.remapped(home = loaded, layout = current).copy(
							source = sourceOf(
								local = local,
								connected = connected,
								signedOut = signedOut,
							),
						)
					},
					onFailure = {
						if (state.content != null) {
							base.copy(
								source = sourceOf(
									local = local,
									connected = connected,
									signedOut = signedOut,
								),
								notice = HomeNotice.RefreshFailed,
							)
						} else {
							base.copy(
								fullState = when {
									local -> HomeFullState.DeviceEmpty
									signedOut -> HomeFullState.SignedOut
									connected -> HomeFullState.Unreachable
									else -> HomeFullState.OfflineEmpty
								},
								source = when {
									local -> HomeSourceUi.ThisDevice
									signedOut -> HomeSourceUi.SignedOut
									connected -> HomeSourceUi.NotResponding
									else -> HomeSourceUi.Offline
								},
							)
						}
					},
				)
			}
		}
	}

	private fun retrySection(key: String) {
		val id = HomeSectionId.fromKey(key = key) ?: return
		val current = layout ?: return
		retryJob?.cancel()
		retryJob = viewModelScope.launch {
			val result = try {
				repository.homeSection(id = id)
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				HomeSectionResult.Failed
			}
			val updated = home?.withSection(
				id = id,
				result = result,
			) ?: return@launch
			home = updated
			_state.update { it.remapped(home = updated, layout = current) }
		}
	}

	private fun signedOut(): Boolean =
		!session.browsingLocalFiles && !Session.isLoggedInState.value && Session.offlineAccount.value != null

	private fun sourceOf(
		local: Boolean,
		connected: Boolean,
		signedOut: Boolean,
	): HomeSourceUi = when {
		local -> HomeSourceUi.ThisDevice
		signedOut -> HomeSourceUi.SignedOut
		connected -> HomeSourceUi.Connected
		else -> HomeSourceUi.Offline
	}

	private fun emptyState(local: Boolean, connected: Boolean): HomeFullState = when {
		local -> HomeFullState.DeviceEmpty
		signedOut() -> HomeFullState.SignedOut
		connected -> HomeFullState.ServerEmpty
		else -> HomeFullState.OfflineEmpty
	}

	private fun undo() {
		when (val notice = _state.value.notice) {
			is HomeNotice.WatchedChanged -> setWatched(
				itemId = notice.itemId,
				title = notice.title,
				watched = !notice.watched,
				announce = false,
			)
			is HomeNotice.RemovedFromContinue -> setHidden(
				itemId = notice.itemId,
				title = notice.title,
				hide = false,
			)
			is HomeNotice.FavoriteChanged,
			is HomeNotice.ActionFailed,
			HomeNotice.RefreshFailed,
			HomeNotice.DownloadQueued,
			is HomeNotice.Message,
			null,
			-> Unit
		}
	}

	private fun setWatched(
		itemId: String,
		title: String,
		watched: Boolean,
		announce: Boolean = true,
	) {
		val before = _state.value.content
		watchedJob?.cancel()
		watchedJob = viewModelScope.launch {
			_state.update { current ->
				val content = current.content?.updateCards(itemId = itemId) { it.copy(isWatched = watched) }
				current.copy(content = if (watched) content?.withoutInProgress(itemId = itemId) else content)
			}
			try {
				repository.togglePlayed(
					itemId = itemId,
					isPlayed = watched,
				)
				_state.update {
					it.copy(
						notice = if (announce) {
							HomeNotice.WatchedChanged(
								itemId = itemId,
								title = title,
								watched = watched,
							)
						} else {
							it.notice?.takeUnless { notice -> notice is HomeNotice.WatchedChanged }
						},
					)
				}
				if (!watched) load(refresh = true)
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				_state.update {
					it.copy(
						content = before,
						notice = HomeNotice.ActionFailed(title = title),
					)
				}
			}
		}
	}

	private fun setHidden(
		itemId: String,
		title: String,
		hide: Boolean,
	) {
		val before = _state.value.content
		hideJob?.cancel()
		hideJob = viewModelScope.launch {
			if (hide) _state.update { current -> current.copy(content = current.content?.withoutInProgress(itemId = itemId)) }
			try {
				repository.hideFromResume(
					itemId = itemId,
					hide = hide,
				)
				_state.update {
					it.copy(
						notice = if (hide) {
							HomeNotice.RemovedFromContinue(
								itemId = itemId,
								title = title,
							)
						} else {
							it.notice?.takeUnless { notice -> notice is HomeNotice.RemovedFromContinue }
						},
					)
				}
				if (!hide) load(refresh = true)
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				_state.update {
					it.copy(
						content = before,
						notice = HomeNotice.ActionFailed(title = title),
					)
				}
			}
		}
	}

	private fun toggleFavorite(itemId: String) {
		val card = _state.value.content?.findCard(itemId = itemId) ?: return
		val target = !card.isFavorite
		favoriteJob?.cancel()
		favoriteJob = viewModelScope.launch {
			_state.update { current -> current.copy(content = current.content?.updateCards(itemId = itemId) { it.copy(isFavorite = target) }) }
			try {
				repository.toggleFavorite(
					itemId = itemId,
					isFavorite = target,
				)
				_state.update {
					it.copy(
						notice = HomeNotice.FavoriteChanged(
							title = card.title,
							added = target,
						),
					)
				}
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				_state.update { current ->
					current.copy(
						content = current.content?.updateCards(itemId = itemId) { it.copy(isFavorite = !target) },
						notice = HomeNotice.ActionFailed(title = card.title),
					)
				}
			}
		}
	}

	private fun requestDownload(itemId: String) {
		val item = home?.findItem(itemId = itemId) ?: return
		viewModelScope.launch {
			try {
				when (val request = downloads.download(item = item)) {
					is DownloadRequest.Choose -> _state.update {
						it.copy(
							downloadChoice = DownloadChoice(
								title = request.title,
								sources = request.sources,
								selected = request.selected,
							),
						)
					}
					is DownloadRequest.Failed -> _state.update { it.copy(notice = HomeNotice.Message(text = request.message)) }
				}
			} catch (exception: CancellationException) {
				throw exception
			} catch (_: Exception) {
				_state.update { it.copy(notice = HomeNotice.ActionFailed(title = item.name)) }
			}
		}
	}

	private fun confirmDownload() {
		val choice = _state.value.downloadChoice ?: return
		val key = choice.selected ?: return
		if (choice.submitting) return
		_state.update { it.copy(downloadChoice = choice.copy(submitting = true)) }
		viewModelScope.launch {
			try {
				val result = downloads.confirm(key = key)
				_state.update { state ->
					if (result.ok) {
						state.copy(
							downloadChoice = null,
							notice = result.message?.let { HomeNotice.Message(text = it) } ?: HomeNotice.DownloadQueued,
						)
					} else {
						state.copy(notice = result.message?.let { HomeNotice.Message(text = it) } ?: state.notice)
					}
				}
			} finally {
				_state.update { it.copy(downloadChoice = it.downloadChoice?.copy(submitting = false)) }
			}
		}
	}
}

private fun EmbyHome.findItem(itemId: String): EmbyItem? = sections.values
	.filterIsInstance<HomeSectionResult.Loaded>()
	.firstNotNullOfOrNull { result -> result.items.firstOrNull { it.id == itemId } }
