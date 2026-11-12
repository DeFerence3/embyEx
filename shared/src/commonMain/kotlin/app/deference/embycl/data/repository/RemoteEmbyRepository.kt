package app.deference.embycl.data.repository

import app.deference.embycl.core.networking.DONT_INTERCEPT
import app.deference.embycl.core.session.Session
import app.deference.embycl.core.utils.NetworkUtils.safeApiCall
import app.deference.embycl.core.utils.buildUrl
import app.deference.embycl.domain.model.EmbyHome
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyItemsResult
import app.deference.embycl.domain.model.EmbyPlaybackEvent
import app.deference.embycl.domain.model.EmbyPlaybackItem
import app.deference.embycl.domain.model.EmbyPlaybackReport
import app.deference.embycl.domain.model.EmbyPlaybackRequest
import app.deference.embycl.domain.model.ServerDetails
import app.deference.embycl.domain.model.ServerInfo
import app.deference.embycl.domain.model.home.HomeLayoutPrefs
import app.deference.embycl.domain.model.home.HomeSectionId
import app.deference.embycl.domain.model.home.HomeSectionResult
import app.deference.embycl.domain.repository.EmbyRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

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
	
	override suspend fun home(layout: HomeLayoutPrefs): EmbyHome = supervisorScope {
		val views = libraries()
		// Downloaded comes from this device (EmbyRepositoryImpl); everything else is asked for in parallel.
		val requested = layout.resolvedFor(libraries = views).visibleSections.filter { it != HomeSectionId.Downloaded }
		val sections = requested
			.map { id -> async { id to section(id = id, startIndex = 0) } }
			.awaitAll()
			.toMap()
		if (sections.isNotEmpty() && sections.values.all { it == HomeSectionResult.Failed }) {
			throw kotlinx.io.IOException("Couldn't load any Home section.")
		}
		EmbyHome(
			libraries = views,
			sections = sections,
		)
	}

	override suspend fun homeSection(id: HomeSectionId, startIndex: Int): HomeSectionResult = section(
		id = id,
		startIndex = startIndex,
	)

	/** One section; any failure other than cancellation becomes [HomeSectionResult.Failed] so the rest of Home still shows. */
	private suspend fun section(id: HomeSectionId, startIndex: Int): HomeSectionResult = try {
		when (id) {
			HomeSectionId.ContinueWatching -> continueWatching(startIndex = startIndex)
			is HomeSectionId.LatestIn -> latest(libraryId = id.libraryId)
			HomeSectionId.Favorites -> favorites(startIndex = startIndex)
			HomeSectionId.Downloaded -> HomeSectionResult.Loaded(items = emptyList(), total = 0)
		}
	} catch (exception: CancellationException) {
		throw exception
	} catch (exception: Exception) {
		HomeSectionResult.Failed
	}

	/**
	 * Resume first (server order, most recently played first), then Next up in server order; a Next up episode is
	 * dropped when its series already has a resumable episode. Next up only on the first page, and its failure doesn't
	 * fail the section (Resume still shows).
	 */
	private suspend fun continueWatching(startIndex: Int): HomeSectionResult = supervisorScope {
		val nextUp = async { if (startIndex == 0) nextUp() else emptyList() }
		val resume = resume(startIndex = startIndex)
		val following = try {
			nextUp.await()
		} catch (exception: CancellationException) {
			throw exception
		} catch (exception: Exception) {
			emptyList()
		}
		val resumeIds = resume.items.map { it.id }.toSet()
		val resumeSeries = resume.items.mapNotNull { it.seriesId }.toSet()
		val added = following.filter { it.id !in resumeIds && it.seriesId !in resumeSeries }
		HomeSectionResult.Loaded(
			items = resume.items + added,
			total = resume.totalRecordCount + added.size,
		)
	}

	private suspend fun nextUp(): List<EmbyItem> = safeApiCall {
		httpClient.get("/Shows/NextUp") {
			mapOf(
				"UserId" to Session.userId,
				"Limit" to "20",
				"Fields" to ITEM_FIELDS,
				"EnableImages" to "true",
				"EnableUserData" to "true",
				"ImageTypeLimit" to "1",
			).forEach { (key, value) -> parameter(key, value) }
		}.body<EmbyItemsResult>()
	}.items

	private suspend fun resume(startIndex: Int): EmbyItemsResult {
		val result = safeApiCall {
			httpClient.get("/Users/${Session.userId}/Items/Resume") {
				mapOf(
					"StartIndex" to startIndex.toString(),
					"Limit" to "20",
					"MediaTypes" to "Video",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (key, value) -> parameter(key, value) }
			}.body<EmbyItemsResult>()
		}
		return result
	}

	/** Newest titles in one library; GroupItems puts a show's new episodes under one series card. Emby returns no total. */
	private suspend fun latest(libraryId: String): HomeSectionResult {
		val items = safeApiCall {
			httpClient.get("/Users/${Session.userId}/Items/Latest") {
				mapOf(
					"ParentId" to libraryId,
					"GroupItems" to "true",
					"Limit" to "24",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (key, value) -> parameter(key, value) }
			}.body<List<EmbyItem>>()
		}
		return HomeSectionResult.Loaded(
			items = items,
			total = null,
		)
	}

	private suspend fun favorites(startIndex: Int): HomeSectionResult {
		val result = safeApiCall {
			httpClient.get("/Users/${Session.userId}/Items") {
				mapOf(
					"Filters" to "IsFavorite",
					"Recursive" to "true",
					"IncludeItemTypes" to "Movie,Series,Episode",
					"SortBy" to "SortName",
					"SortOrder" to "Ascending",
					"StartIndex" to startIndex.toString(),
					"Limit" to "24",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				).forEach { (key, value) -> parameter(key, value) }
			}.body<EmbyItemsResult>()
		}
		return HomeSectionResult.Loaded(
			items = result.items,
			total = result.totalRecordCount,
		)
	}

	override suspend fun hideFromResume(itemId: String, hide: Boolean) {
		safeApiCall {
			httpClient.post("/Users/${Session.userId}/Items/${itemId}/HideFromResume") {
				parameter("Hide", hide.toString())
			}
		}
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
	
    override suspend fun play(item: EmbyItem): EmbyPlaybackRequest {
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
        val season = if (siblings.any { it.id == item.id }) {
            siblings.sortedWith(
                comparator = compareBy<EmbyItem> { it.parentIndexNumber ?: 0 }
                    .thenBy { it.indexNumber ?: 0 }
                    .thenBy { it.name.lowercase() },
            )
        } else listOf(item)
        val playlist = season + listOfNotNull(nextSeasonOpener(item = item, season = season))
        return EmbyPlaybackRequest(
            items = playlist.map { entry ->
                EmbyPlaybackItem(
                    id = entry.id,
                    title = entry.name,
                    url = streamUrl(item = entry),
                    positionMs = (entry.userData?.playbackPositionTicks ?: 0L).coerceAtLeast(0L) / 10_000L,
                    artworkUrl = entry.imageUrl(type = "Backdrop", maxWidth = 1280)
                        ?: entry.imageUrl(type = "Primary", maxWidth = 1280),
                    episodeLabel = entry.episodeLabel(),
                    seriesName = entry.seriesName.takeIf { entry.isEpisode() },
                    durationMs = entry.runTimeTicks?.let { it / 10_000L },
                )
            },
            selectedIndex = playlist.indexOfFirst { it.id == item.id },
            serverUrl = server,
            userId = user,
            accessToken = token,
            sessionId = Uuid.random().toString(),
        )
    }

	/**
	 * The episode after [season]'s last one when that is in a later season, from the server's adjacent-episode
	 * query. Null when [item] isn't an episode, when the series ends there, or when the query fails: Up next then
	 * stops at the season finale, which is what the player did before.
	 */
	private suspend fun nextSeasonOpener(
		item: EmbyItem,
		season: List<EmbyItem>,
	): EmbyItem? {
		val last = season.lastOrNull() ?: return null
		val series = item.seriesId
		if (!item.isEpisode() || series.isNullOrBlank()) return null
		return try {
			val adjacent = safeApiCall {
				httpClient.get("/Shows/$series/Episodes") {
					mapOf(
						"UserId" to Session.userId,
						"AdjacentTo" to last.id,
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					).forEach { (name, value) -> parameter(name, value) }
				}.body<EmbyItemsResult>()
			}.items
			// AdjacentTo returns [previous, current, next]; only a next one in another season is new.
			adjacent.dropWhile { it.id != last.id }.drop(n = 1).firstOrNull()
				?.takeIf { next -> next.isEpisode() && season.none { it.id == next.id } }
		} catch (exception: kotlinx.coroutines.CancellationException) {
			throw exception
		} catch (_: Exception) {
			null
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
