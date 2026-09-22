package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyItemsResult
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.domain.model.ServerDetails

interface EmbyRepository {
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
