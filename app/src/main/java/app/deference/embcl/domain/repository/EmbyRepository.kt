package app.deference.embcl.domain.repository

import app.deference.embcl.domain.model.EmbyHome
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbyPlaybackEvent
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.EmbyUser

interface EmbyRepository {
	suspend fun authenticate(server: String, username: String, password: String): EmbySession
	suspend fun discoverServer(server: String): EmbyServerDiscovery
	suspend fun discoverLocalServers(): List<app.deference.embcl.domain.model.EmbyUdpServer>
	fun getSavedServerUrl(): String?
	suspend fun authenticate(discovery: EmbyServerDiscovery, username: String, password: String): EmbySession
	suspend fun authenticate(discovery: EmbyServerDiscovery, user: EmbyUser, password: String): EmbySession
	fun publicUserImageUrl(discovery: EmbyServerDiscovery, user: EmbyUser): String?
	suspend fun home(): EmbyHome
	suspend fun libraries(): List<EmbyItem>
	suspend fun items(parentId: String, startIndex: Int = 0): EmbyItemsResult
	suspend fun search(term: String): List<EmbyItem>
	suspend fun item(id: String): EmbyItem
	fun imageUrl(item: EmbyItem, type: String = "Primary", maxWidth: Int = 600): String?
	fun userImageUrl(): String
	fun streamUrl(item: EmbyItem): String
	fun logout()
	suspend fun toggleFavorite(itemId: String, isFavorite: Boolean)
	suspend fun togglePlayed(itemId: String, isPlayed: Boolean)
	fun reportPlayback(itemId: String, positionTicks: Long, event: EmbyPlaybackEvent, isPaused: Boolean = false)
}
