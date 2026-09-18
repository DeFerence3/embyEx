package app.deference.embcl.data.repository

import app.deference.embcl.core.networking.ApiResponseHandler.safeApiCall
import app.deference.embcl.core.session.Session
import app.deference.embcl.domain.model.EmbyHome
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbyPlaybackEvent
import app.deference.embcl.domain.model.EmbyPlaybackReport
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.EmbyRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.core.annotation.Single

@Single
class EmbyRepositoryImpl(
	private val httpClient: HttpClient,
) : EmbyRepository {
	/*	val ession by lazy {
			sessionStore.Session.value ?: throw IllegalStateException("No Session found")
		}*/
	override fun getSavedServerUrl(): String? = Session.getLastServerUrl()
	
	override fun publicUserImageUrl(discovery: EmbyServerDiscovery, user: EmbyUser): String? {
		val tag = user.primaryImageTag ?: return null
		return buildUrl(
			discovery.serverUrl,
			"/Users/${user.id}/Images/Primary",
			mapOf("MaxWidth" to "192", "Quality" to "90", "Tag" to tag),
		).toString()
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
				/*api.resumeItems(
					userId = Session.userId,
					options = mapOf(
						"Limit" to "20",
						"MediaTypes" to "Video",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					),
				)*/
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
				/*api.latestItems(
					userId = Session.userId,
					options = mapOf(
						"Limit" to "24",
						"IncludeItemTypes" to "Movie,Episode",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					),
				)*/
			}
		}
		EmbyHome(views.await(), resume.await(), latest.await())
	}
	
	override suspend fun libraries(): List<EmbyItem> = safeApiCall {
		httpClient.get("/Users/${Session.userId}/Views") {
			parameter("IncludeExternalContent", false)
		}.body<EmbyItemsResult>().items
		/*return safeApiCall {
			api.userViews(
				userId = Session.userId,
				options = mapOf("IncludeExternalContent" to "false"),
			)
		}.items*/
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
					//"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (name, value) -> parameter(name, value) }
			}.body<EmbyItemsResult>()
			/*api.userItems(
				userId = Session.userId,
				options = mapOf(
					"ParentId" to parentId,
					"StartIndex" to startIndex.toString(),
					"Limit" to "100",
					"SortBy" to "SortName",
					"SortOrder" to "Ascending",
					//"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				),
			)*/
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
			/*api.userItems(
				userId = Session.userId,
				options = mapOf(
					"SearchTerm" to term.trim(),
					"Recursive" to "true",
					"Limit" to "60",
					"IncludeItemTypes" to "Movie,Series,Season,Episode,Video",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				),
			)*/
		}.items
	}
	
	override suspend fun item(id: String): EmbyItem {
		return safeApiCall {
			httpClient
				.get("/Users/${Session.userId}/Items/${id}")
				.body<EmbyItem>()
			/*api.item(
				userId = Session.userId,
				itemId = id
			)*/
		}
	}
	
	override fun imageUrl(
		item: EmbyItem,
		type: String,
		maxWidth: Int,
	): String? {
		val imageItemId = when (type) {
			"Backdrop" if item.backdropImageTags.isNotEmpty() -> item.id
			"Backdrop" if item.parentBackdropImageTags.isNotEmpty() -> item.parentBackdropItemId
			"Logo" if item.imageTags.containsKey("Logo") -> item.id
			"Logo" if ! item.parentLogoItemId.isNullOrBlank() -> item.parentLogoItemId
			"Primary" if item.imageTags.containsKey("Primary") -> item.id
			else -> null
		} ?: return null
		val tag = when (type) {
			"Backdrop" -> item.backdropImageTags.firstOrNull() ?: item.parentBackdropImageTags.firstOrNull()
			"Logo" -> item.imageTags["Logo"] ?: item.parentLogoImageTag
			else -> item.imageTags[type]
		}
		return buildUrl(
			Session.serverUrl,
			"/Items/$imageItemId/Images/$type",
			mapOf(
				"MaxWidth" to maxWidth.toString(),
				"Quality" to "90",
				"Tag" to tag,
				"api_key" to Session.accessToken,
			),
		).toString()
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
//				api.markFavorite(Session.userId, itemId)
			} else {
				httpClient.delete("/Users/${Session.userId}/FavoriteItems/${itemId}")
//				api.unmarkFavorite(Session.userId, itemId)
			}
		}
	}
	
	override suspend fun togglePlayed(itemId: String, isPlayed: Boolean) {
		safeApiCall {
			if (isPlayed) {
				httpClient.post("/Users/${Session.userId}/PlayedItems/${itemId}")
//				api.markPlayed(Session.userId, itemId)
			} else {
				httpClient.delete("/Users/${Session.userId}/PlayedItems/${itemId}")
//				api.unmarkPlayed(Session.userId, itemId)
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
//				api.reportPlayback(report)
			}
			
			EmbyPlaybackEvent.Progress -> suspend {
				httpClient.post("/Sessions/Playing/Progress") {
					setBody(report)
				}
//				api.reportPlaybackProgress(report)
			}
			
			EmbyPlaybackEvent.Stopped -> suspend {
				httpClient.post("/Sessions/Playing/Stopped") {
					setBody(report)
				}
//				api.reportPlaybackStopped(report)
			}
		}
		
		CoroutineScope(Dispatchers.IO).launch {
			call.invoke()
		}
		/*call.enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
			override fun onFailure(call: Call<Void>, error: Throwable) = Unit
		})*/
	}
	
	private fun buildUrl(base: String, path: String, parameters: Map<String, String?>): HttpUrl {
		val baseUrl = base.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid Emby server address.")
		return baseUrl.newBuilder().addPathSegments(path.trimStart('/')).apply {
			parameters.forEach { (name, value) -> value?.let { addQueryParameter(name, it) } }
		}.build()
	}
	
	private companion object {
		
		const val ITEM_FIELDS = "Overview,ProductionYear,CommunityRating,OfficialRating,RunTimeTicks,Genres,MediaSources,MediaStreams,ParentId,PrimaryImageAspectRatio"
	}
}
