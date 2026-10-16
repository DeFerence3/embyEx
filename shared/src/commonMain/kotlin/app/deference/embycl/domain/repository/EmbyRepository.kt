package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyItemsResult
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.domain.model.EmbyPlaybackRequest
import app.deference.embycl.domain.model.ServerDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface EmbyRepository {
    val changes: Flow<Unit> get() = emptyFlow()
    /** Returns an external-player request, or null when playback was handled by the repository. */
    suspend fun play(item: EmbyItem): EmbyPlaybackRequest? = null
    /** Streams from the server even when a saved copy exists. */
    suspend fun playFromServer(item: EmbyItem): EmbyPlaybackRequest? = play(item)
	suspend fun serverInfo(): ServerDetails
	suspend fun home(): EmbyHome
	suspend fun libraries(): List<EmbyItem>
	suspend fun items(parentId: String, startIndex: Int = 0): EmbyItemsResult
	suspend fun search(term: String): List<EmbyItem>
	suspend fun item(id: String): EmbyItem
	fun userImageUrl(): String
	fun streamUrl(item: EmbyItem): String
	suspend fun toggleFavorite(itemId: String, isFavorite: Boolean)
	suspend fun togglePlayed(itemId: String, isPlayed: Boolean)
	fun reportPlayback(itemId: String, positionTicks: Long, event: EmbyPlaybackEvent, isPaused: Boolean = false)
}
