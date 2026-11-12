package app.deference.embycl.ui.screens.settings.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeLayoutSync
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.domain.repository.EmbyRepository
import app.deference.embycl.domain.repository.HomeLayoutRepository
import app.deference.embycl.domain.repository.OfflineUiSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/**
 * Edit Home in Settings. Changes go through [HomeLayoutRepository], so Home follows them as they are made.
 * Libraries come from the server when connected and from this device otherwise; while offline an edit keeps the rows of
 * server libraries this device doesn't know, so their place and visibility survive.
 */
@KoinViewModel
class HomeLayoutSettingsViewModel(
    private val layoutRepository: HomeLayoutRepository,
    private val repository: EmbyRepository,
    private val session: OfflineUiSession,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeLayoutSettingsState())
    val state: StateFlow<HomeLayoutSettingsState> = _state.asStateFlow()
    private val libraries = MutableStateFlow<List<EmbyItem>?>(null)
    private var saved: HomeLayoutPrefs? = null
    private var beforeReset: HomeLayoutPrefs? = null

    init {
        viewModelScope.launch {
            session.connected.collect { loadLibraries() }
        }
        viewModelScope.launch {
            combine(
                layoutRepository.layout,
                libraries.filterNotNull(),
                layoutRepository.sync,
                session.connected,
            ) { layout, libraries, sync, _ ->
                saved = layout
                val canSave = canSaveToServer()
                val resolved = layout.resolvedFor(
                    libraries = libraries,
                    keepMissingLibraries = !canSave,
                )
                HomeLayoutSettingsState(
                    isLoading = false,
                    top = resolved.top,
                    cardSize = resolved.cardSize,
                    rows = resolved.toLayoutRows(libraries = libraries),
                    sync = if (canSave) sync else HomeLayoutSync.LocalOnly,
                )
            }.collect { next -> _state.update { next.copy(resetNotice = it.resetNotice) } }
        }
    }

    fun onAction(action: HomeLayoutSettingsAction) {
        when (action) {
            is HomeLayoutSettingsAction.SetTop -> edit { it.copy(top = action.top) }
            is HomeLayoutSettingsAction.SetCardSize -> edit { it.copy(cardSize = action.size) }
            is HomeLayoutSettingsAction.ToggleSection -> edit { current ->
                current.copy(sections = current.sections.map { if (it.id.key == action.key) it.copy(visible = !it.visible) else it })
            }
            is HomeLayoutSettingsAction.MoveSection -> edit { current ->
                current.copy(
                    sections = current.sections.moved(
                        key = action.key,
                        up = action.up,
                        pinnedFirst = current.top == HomeTop.ContinueWatching,
                    ),
                )
            }
            HomeLayoutSettingsAction.Reset -> {
                beforeReset = saved
                viewModelScope.launch {
                    layoutRepository.reset()
                    _state.update { it.copy(resetNotice = true) }
                }
            }
            HomeLayoutSettingsAction.UndoReset -> {
                val previous = beforeReset ?: return
                beforeReset = null
                viewModelScope.launch { layoutRepository.update { previous } }
            }
            HomeLayoutSettingsAction.ResetNoticeShown -> _state.update { it.copy(resetNotice = false) }
        }
    }

    private fun edit(transform: (HomeLayoutPrefs) -> HomeLayoutPrefs) {
        val known = libraries.value.orEmpty()
        val keepMissing = !canSaveToServer()
        viewModelScope.launch {
            layoutRepository.update { current ->
                transform(
                    current.resolvedFor(
                        libraries = known,
                        keepMissingLibraries = keepMissing,
                    ),
                )
            }
        }
    }

    /** Libraries for the "New in" rows; a failed load keeps the last list (or none) so the fixed sections still show. */
    private suspend fun loadLibraries() {
        val loaded = try {
            repository.libraries()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
        libraries.value = loaded ?: libraries.value ?: emptyList()
    }

    private fun canSaveToServer(): Boolean =
        !session.browsingLocalFiles && session.connected.value && Session.isLoggedInState.value
}
