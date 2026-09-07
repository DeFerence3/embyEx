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
	suspend fun authenticate(discovery: EmbyServerDiscovery, username: String, password: String): EmbySession
	suspend fun authenticate(discovery: EmbyServerDiscovery, user: EmbyUser, password: String): EmbySession
	fun publicUserImageUrl(discovery: EmbyServerDiscovery, user: EmbyUser): String?
	suspend fun home(session: EmbySession): EmbyHome
	suspend fun libraries(session: EmbySession): List<EmbyItem>
	suspend fun items(session: EmbySession, parentId: String, startIndex: Int = 0): EmbyItemsResult
	suspend fun search(session: EmbySession, term: String): List<EmbyItem>
	suspend fun item(session: EmbySession, id: String): EmbyItem
	fun imageUrl(session: EmbySession, item: EmbyItem, type: String = "Primary", maxWidth: Int = 600): String?
	fun userImageUrl(session: EmbySession): String
	fun streamUrl(session: EmbySession, item: EmbyItem): String
	fun logout()
	fun reportPlayback(itemId: String, positionTicks: Long, event: EmbyPlaybackEvent, isPaused: Boolean = false)
}
