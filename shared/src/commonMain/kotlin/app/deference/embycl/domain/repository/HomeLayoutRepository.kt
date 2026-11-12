package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeLayoutSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HomeLayoutRepository {
    val layout: Flow<HomeLayoutPrefs>
    val sync: StateFlow<HomeLayoutSync>
    suspend fun update(transform: (HomeLayoutPrefs) -> HomeLayoutPrefs)
    suspend fun reset()
}
