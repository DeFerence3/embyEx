package app.deference.embycl.ui.screens.home.seeall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionResult
import app.deference.embycl.domain.repository.EmbyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class HomeSectionViewModel(
    @InjectedParam private val key: String,
    private val repository: EmbyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeSectionState())
    val state: StateFlow<HomeSectionState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load(startIndex = 0)
    }

    fun onAction(action: HomeSectionAction) {
        when (action) {
            HomeSectionAction.Retry -> {
                _state.value = HomeSectionState()
                load(startIndex = 0)
            }
            HomeSectionAction.LoadMore -> if (_state.value.canLoadMore) load(startIndex = _state.value.items.orEmpty().size)
        }
    }

    private fun load(startIndex: Int) {
        val id = HomeSectionId.fromKey(key = key) ?: return _state.update { it.copy(failed = true, items = it.items.orEmpty()) }
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loadingMore = startIndex > 0) }
            val result = try {
                repository.homeSection(
                    id = id,
                    startIndex = startIndex,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                HomeSectionResult.Failed
            }
            _state.update { current ->
                when (result) {
                    is HomeSectionResult.Loaded -> current.copy(
                        items = current.items.orEmpty().takeIf { startIndex > 0 }.orEmpty() + result.items,
                        total = result.total,
                        failed = false,
                        loadingMore = false,
                    )
                    HomeSectionResult.Failed -> current.copy(
                        items = current.items ?: emptyList(),
                        failed = true,
                        loadingMore = false,
                    )
                }
            }
        }
    }
}
