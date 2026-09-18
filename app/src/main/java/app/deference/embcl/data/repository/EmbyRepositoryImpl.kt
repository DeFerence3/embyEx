package app.deference.embcl.data.repository

import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.NetworkUtils.safeApiCall
import app.deference.embcl.core.utils.buildUrl
import app.deference.embcl.domain.model.EmbyHome
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbyPlaybackEvent
import app.deference.embcl.domain.model.EmbyPlaybackReport
import app.deference.embcl.domain.model.ServerDetails
import app.deference.embcl.domain.model.ServerInfo
import app.deference.embcl.domain.repository.EmbyRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class EmbyRepositoryImpl(
	private val httpClient: HttpClient,
) : EmbyRepository {
	
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
				"api_key" to Session.accessToken,
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
	
	override fun reportPlayback(
		itemId: String,
		positionTicks: Long,
		event: EmbyPlaybackEvent,
		isPaused: Boolean,
	) {
		val playSessionId = "${Session.deviceId}_$itemId"
		val report = EmbyPlaybackReport(
			itemId = itemId,
			positionTicks = positionTicks.coerceAtLeast(0),
			playSessionId = playSessionId,
			isPaused = isPaused,
		)
		val call = when (event) {
			EmbyPlaybackEvent.Started -> suspend {
				httpClient.post("/Sessions/Playing") {
					setBody(report)
				}
			}
			
			EmbyPlaybackEvent.Progress -> suspend {
				httpClient.post("/Sessions/Playing/Progress") {
					setBody(report)
				}
			}
			
			EmbyPlaybackEvent.Stopped -> suspend {
				httpClient.post("/Sessions/Playing/Stopped") {
					setBody(report)
				}
			}
		}
		
		CoroutineScope(Dispatchers.IO).launch {
			call.invoke()
		}
	}
	
	private companion object {
		
		const val ITEM_FIELDS = "Overview,ProductionYear,CommunityRating,OfficialRating,RunTimeTicks,Genres,MediaSources,MediaStreams,ParentId,PrimaryImageAspectRatio"
	}
}
