package app.deference.embycl.data.repository

import app.deference.embycl.core.session.Session
import app.deference.embycl.core.utils.NetworkUtils.safeApiCall
import app.deference.embycl.core.utils.buildUrl
import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyItemsResult
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.core.networking.DONT_INTERCEPT
import app.deference.embycl.domain.model.EmbyPlaybackItem
import app.deference.embycl.domain.model.EmbyPlaybackRequest
import kotlin.uuid.Uuid
import io.ktor.client.request.header
import app.deference.embycl.domain.model.EmbyPlaybackReport
import app.deference.embycl.domain.model.ServerDetails
import app.deference.embycl.domain.model.ServerInfo
import app.deference.embycl.domain.repository.EmbyRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koin.core.annotation.Single

@Single(binds = [RemoteEmbyRepository::class])
class RemoteEmbyRepository(
	private val httpClient: HttpClient,
) : EmbyRepository {
    private val playbackChanges = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val changes = playbackChanges.asSharedFlow()

	
	override suspend fun serverInfo(): ServerDetails {
		var response = httpClient.get("/System/Info") { expectSuccess = false }
		val isLimited = response.status == HttpStatusCode.Forbidden
		if (isLimited) {
			response = httpClient.get("/System/Info/Public") { expectSuccess = false }
		}
		if (!response.status.isSuccess()) {
			throw kotlinx.io.IOException("Could not load server information (HTTP ${response.status.value}).")
		}
		return ServerDetails(response.body<ServerInfo>(), isLimited)
	}
	
	override suspend fun home(): EmbyHome = coroutineScope {
		val views = async { libraries() }
		val resume = async {
			safeApiCall {
				httpClient.get("/Users/${Session.userId}/Items/Resume") {
					mapOf(
						"Limit" to "20",
						"MediaTypes" to "Video",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					).forEach { (key, value) -> parameter(key, value) }
				}.body<EmbyItemsResult>()
			}.items
		}
		val latest = async {
			safeApiCall {
				httpClient.get("/Users/${Session.userId}/Items/Latest") {
					mapOf(
						"Limit" to "24",
						"IncludeItemTypes" to "Movie,Episode",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					).forEach { (key, value) -> parameter(key, value) }
				}.body<List<EmbyItem>>()
			}
		}
		EmbyHome(views.await(), resume.await(), latest.await())
	}
	
	override suspend fun libraries(): List<EmbyItem> = safeApiCall {
		httpClient.get("/Users/${Session.userId}/Views") {
			parameter("IncludeExternalContent", false)
		}.body<EmbyItemsResult>().items
	}
	
	override suspend fun items(
		parentId: String,
		startIndex: Int,
	): EmbyItemsResult {
		return safeApiCall {
			httpClient.get("/Users/${Session.userId}/Items") {
				mapOf(
					"ParentId" to parentId,
					"StartIndex" to startIndex.toString(),
					"Limit" to "100",
					"SortBy" to "SortName",
					"SortOrder" to "Ascending",
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (name, value) -> parameter(name, value) }
			}.body<EmbyItemsResult>()
		}
	}
	
	override suspend fun search(term: String): List<EmbyItem> {
		if (term.isBlank()) return emptyList()
		return safeApiCall {
			httpClient.get("/Users/${Session.userId}/Items") {
				mapOf(
					"SearchTerm" to term.trim(),
					"Recursive" to "true",
					"Limit" to "60",
					"IncludeItemTypes" to "Movie,Series,Season,Episode,Video",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (name, value) -> parameter(name, value) }
			}.body<EmbyItemsResult>()
		}.items
	}
	
	override suspend fun item(id: String): EmbyItem {
		return safeApiCall {
			httpClient
				.get("/Users/${Session.userId}/Items/${id}")
				.body<EmbyItem>()
		}
	}
	
    override suspend fun play(item: EmbyItem): app.deference.embycl.domain.model.EmbyPlaybackRequest {
        val token = Session.accessToken ?: error("Sign in to play this item.")
        val server = Session.serverUrl
        val user = Session.userId
        val parent = item.seasonId ?: item.parentId
        val siblings = if (item.isEpisode() && !parent.isNullOrBlank()) {
            try { items(parent).items.ifEmpty { listOf(item) } }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { listOf(item) }
        } else listOf(item)
        check(server == Session.serverUrl && user == Session.userId && token == Session.accessToken) {
            "Playback account changed."
        }
        val playlist = if (siblings.any { it.id == item.id }) siblings else listOf(item)
        return EmbyPlaybackRequest(
            items = playlist.map { entry ->
                EmbyPlaybackItem(
                    id = entry.id,
                    title = entry.name,
                    url = streamUrl(item = entry),
                    positionMs = (entry.userData?.playbackPositionTicks ?: 0L).coerceAtLeast(0L) / 10_000L,
                )
            },
            selectedIndex = playlist.indexOfFirst { it.id == item.id },
            serverUrl = server,
            userId = user,
            accessToken = token,
            sessionId = Uuid.random().toString(),
        )
    }

	override fun userImageUrl(): String = buildUrl(
		Session.serverUrl,
		"/Users/${Session.userId}/Images/Primary",
		mapOf("MaxWidth" to "160"),
	).toString()
	
	override fun streamUrl(item: EmbyItem): String {
		val extension = item.container?.substringBefore(',')?.ifBlank { null } ?: "mkv"
		return buildUrl(
			Session.serverUrl,
			"/Videos/${item.id}/stream.$extension",
			mapOf(
				"Static" to "true",
				"DeviceId" to Session.deviceId,
			),
		).toString()
	}
	
	override suspend fun toggleFavorite(itemId: String, isFavorite: Boolean) {
		safeApiCall {
			if (isFavorite) {
				httpClient.post("/Users/${Session.userId}/FavoriteItems/${itemId}")
			} else {
				httpClient.delete("/Users/${Session.userId}/FavoriteItems/${itemId}")
			}
		}
	}
	
	override suspend fun togglePlayed(itemId: String, isPlayed: Boolean) {
		safeApiCall {
			if (isPlayed) {
				httpClient.post("/Users/${Session.userId}/PlayedItems/${itemId}")
			} else {
				httpClient.delete("/Users/${Session.userId}/PlayedItems/${itemId}")
			}
		}
	}
	
    override suspend fun reportPlayback(
        request: EmbyPlaybackRequest,
        positionTicks: Long,
        event: EmbyPlaybackEvent,
        isPaused: Boolean,
    ) {
        check(request.serverUrl == Session.serverUrl && request.userId == Session.userId &&
            request.accessToken == Session.accessToken) { "Playback account changed." }
        val report = EmbyPlaybackReport(
            itemId = request.item.id,
            positionTicks = positionTicks.coerceAtLeast(minimumValue = 0L),
            playSessionId = request.sessionId,
            isPaused = isPaused,
        )
        val path = when (event) {
            EmbyPlaybackEvent.Started -> "/Sessions/Playing"
            EmbyPlaybackEvent.Progress -> "/Sessions/Playing/Progress"
            EmbyPlaybackEvent.Stopped -> "/Sessions/Playing/Stopped"
        }
        val response = httpClient.post(urlString = buildUrl(base = request.serverUrl, path = path, qParameters = emptyMap()).toString()) {
            attributes.put(key = DONT_INTERCEPT, value = true)
            header("X-Emby-Token", request.accessToken)
            setBody(body = report)
        }
        check(response.status.isSuccess()) { "The server did not accept playback progress." }
        if (event == EmbyPlaybackEvent.Stopped) playbackChanges.tryEmit(value = Unit)
    }

	private companion object {
		
		const val ITEM_FIELDS = "Overview,ProductionYear,CommunityRating,OfficialRating,RunTimeTicks,Genres,MediaSources,MediaStreams,ParentId,PrimaryImageAspectRatio"
	}
}
