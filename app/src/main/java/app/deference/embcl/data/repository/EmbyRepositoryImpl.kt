package app.deference.embcl.data.repository

import app.deference.embcl.core.networking.ApiResponseHandler.safeApiCall
import app.deference.embcl.core.networking.EmbyMdnsDiscovery
import app.deference.embcl.core.networking.EmbyUdpDiscovery
import app.deference.embcl.core.networking.HostSelectionInterceptor
import app.deference.embcl.core.session.EmbySessionStore
import app.deference.embcl.data.remote.EmbyApiService
import app.deference.embcl.domain.model.AuthenticateRequest
import app.deference.embcl.domain.model.AuthenticateUserRequest
import app.deference.embcl.domain.model.AuthenticationResult
import app.deference.embcl.domain.model.EmbyHome
import app.deference.embcl.domain.model.EmbyItem
import app.deference.embcl.domain.model.EmbyItemsResult
import app.deference.embcl.domain.model.EmbyPlaybackEvent
import app.deference.embcl.domain.model.EmbyPlaybackReport
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.EmbyUdpServer
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.EmbyRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EmbyRepositoryImpl(
	private val api: EmbyApiService,
	private val sessionStore: EmbySessionStore,
	private val hostSelectionInterceptor: HostSelectionInterceptor,
	private val udpDiscovery: EmbyUdpDiscovery,
	private val mdnsDiscovery: EmbyMdnsDiscovery,
) : EmbyRepository {
	override suspend fun authenticate(server: String, username: String, password: String): EmbySession =
		authenticate(discoverServer(server), username, password)
	
	override suspend fun discoverLocalServers(): List<EmbyUdpServer> {
		val udpServers = udpDiscovery.discover()
		if (udpServers.isNotEmpty()) {
			return udpServers
		}
		return mdnsDiscovery.discover()
	}
	
	override fun getSavedServerUrl(): String? = sessionStore.getLastServerUrl()
	
	override suspend fun discoverServer(server: String): EmbyServerDiscovery {
		val serverUrl = normalizeServer(server)
		val deviceId = sessionStore.deviceId()
		
		hostSelectionInterceptor.hostUrl = serverUrl
		
		return try {
			val publicInfo = safeApiCall { api.publicSystemInfo() }
			val users = safeApiCall { api.publicUsers() }
			sessionStore.saveLastServerUrl(serverUrl)
			EmbyServerDiscovery(serverUrl, publicInfo, users, deviceId)
		} catch (e: Exception) {
			hostSelectionInterceptor.hostUrl = serverUrl
			throw e
		}
	}
	
	override suspend fun authenticate(
		discovery: EmbyServerDiscovery,
		username: String,
		password: String,
	): EmbySession {
		hostSelectionInterceptor.hostUrl = discovery.serverUrl
		val result = safeApiCall {
			api.authenticateByName(
				request = AuthenticateRequest(username.trim(), password),
			)
		}
		return createSession(discovery, result)
	}
	
	override suspend fun authenticate(
		discovery: EmbyServerDiscovery,
		user: EmbyUser,
		password: String,
	): EmbySession {
		hostSelectionInterceptor.hostUrl = discovery.serverUrl
		val result = safeApiCall {
			api.authenticateUser(
				userId = user.id,
				request = AuthenticateUserRequest(password),
			)
		}
		return createSession(discovery, result)
	}
	
	override fun publicUserImageUrl(discovery: EmbyServerDiscovery, user: EmbyUser): String? {
		val tag = user.primaryImageTag ?: return null
		return buildUrl(
			discovery.serverUrl,
			"/Users/${user.id}/Images/Primary",
			mapOf("MaxWidth" to "192", "Quality" to "90", "Tag" to tag),
		).toString()
	}
	
	override suspend fun home(session: EmbySession): EmbyHome = coroutineScope {
		hostSelectionInterceptor.hostUrl = session.serverUrl
		val views = async { libraries(session) }
		val resume = async {
			safeApiCall {
				api.resumeItems(
					userId = session.userId,
					options = mapOf(
						"Limit" to "20",
						"MediaTypes" to "Video",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					),
				)
			}.items
		}
		val latest = async {
			safeApiCall {
				api.latestItems(
					userId = session.userId,
					options = mapOf(
						"Limit" to "24",
						"IncludeItemTypes" to "Movie,Episode",
						"Fields" to ITEM_FIELDS,
						"EnableImages" to "true",
						"EnableUserData" to "true",
						"ImageTypeLimit" to "1",
					),
				)
			}
		}
		EmbyHome(views.await(), resume.await(), latest.await())
	}
	
	override suspend fun libraries(session: EmbySession): List<EmbyItem> {
		hostSelectionInterceptor.hostUrl = session.serverUrl
		return safeApiCall {
			api.userViews(
				userId = session.userId,
				options = mapOf("IncludeExternalContent" to "false"),
			)
		}.items
	}
	
	override suspend fun items(
		session: EmbySession,
		parentId: String,
		startIndex: Int,
	): EmbyItemsResult {
		hostSelectionInterceptor.hostUrl = session.serverUrl
		return safeApiCall {
			api.userItems(
				userId = session.userId,
				options = mapOf(
					"ParentId" to parentId,
					"StartIndex" to startIndex.toString(),
					"Limit" to "100",
					"SortBy" to "SortName",
					"SortOrder" to "Ascending",
					"Fields" to ITEM_FIELDS,
					"EnableImages" to "true",
					"EnableUserData" to "true",
					"ImageTypeLimit" to "1",
				),
			)
		}
	}
	
	override suspend fun search(session: EmbySession, term: String): List<EmbyItem> {
		if (term.isBlank()) return emptyList()
		hostSelectionInterceptor.hostUrl = session.serverUrl
		return safeApiCall {
			api.userItems(
				userId = session.userId,
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
			)
		}.items
	}
	
	override suspend fun item(session: EmbySession, id: String): EmbyItem {
		hostSelectionInterceptor.hostUrl = session.serverUrl
		return safeApiCall {
			api.item(
				userId = session.userId,
				itemId = id
			)
		}
	}
	
	override fun imageUrl(
		session: EmbySession,
		item: EmbyItem,
		type: String,
		maxWidth: Int,
	): String? {
		val imageItemId = when (type) {
			"Backdrop" if item.backdropImageTags.isNotEmpty() -> item.id
			"Backdrop" if item.parentBackdropImageTags.isNotEmpty() -> item.parentBackdropItemId
			"Primary" if item.imageTags.containsKey("Primary") -> item.id
			else -> null
		} ?: return null
		val tag = when (type) {
			"Backdrop" -> item.backdropImageTags.firstOrNull() ?: item.parentBackdropImageTags.firstOrNull()
			else -> item.imageTags[type]
		}
		return buildUrl(
			session.serverUrl,
			"/Items/$imageItemId/Images/$type",
			mapOf(
				"MaxWidth" to maxWidth.toString(),
				"Quality" to "90",
				"Tag" to tag,
				"api_key" to session.accessToken,
			),
		).toString()
	}
	
	override fun userImageUrl(session: EmbySession): String = buildUrl(
		session.serverUrl,
		"/Users/${session.userId}/Images/Primary",
		mapOf("MaxWidth" to "160", "api_key" to session.accessToken),
	).toString()
	
	override fun streamUrl(session: EmbySession, item: EmbyItem): String {
		val extension = item.container?.substringBefore(',')?.ifBlank { null } ?: "mkv"
		return buildUrl(
			session.serverUrl,
			"/Videos/${item.id}/stream.$extension",
			mapOf("Static" to "true", "DeviceId" to session.deviceId),
		).toString()
	}
	
	override fun logout() {
		sessionStore.clear()
		hostSelectionInterceptor.hostUrl = null
	}
	
	override fun reportPlayback(
		itemId: String,
		positionTicks: Long,
		event: EmbyPlaybackEvent,
		isPaused: Boolean,
	) {
		val session = sessionStore.session.value ?: return
		hostSelectionInterceptor.hostUrl = session.serverUrl
		val report = EmbyPlaybackReport(itemId, positionTicks.coerceAtLeast(0), isPaused)
		val call = when (event) {
			EmbyPlaybackEvent.Started -> api.reportPlayback(report)
			EmbyPlaybackEvent.Progress -> api.reportPlaybackProgress(report)
			EmbyPlaybackEvent.Stopped -> api.reportPlaybackStopped(report)
		}
		call.enqueue(object : Callback<Void> {
			override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
			override fun onFailure(call: Call<Void>, error: Throwable) = Unit
		})
	}
	
	private fun createSession(
		discovery: EmbyServerDiscovery,
		result: AuthenticationResult,
	): EmbySession = EmbySession(
		serverUrl = discovery.serverUrl,
		serverName = discovery.serverInfo.serverName,
		serverId = result.serverId.ifBlank { discovery.serverInfo.id },
		userId = result.user.id,
		userName = result.user.name,
		accessToken = result.accessToken,
		deviceId = discovery.deviceId,
	).also {
		sessionStore.save(it)
		hostSelectionInterceptor.hostUrl = it.serverUrl
	}
	
	private fun normalizeServer(input: String): String {
		val trimmed = input.trim().trimEnd('/')
		require(trimmed.isNotBlank()) { "Enter your Emby server address." }
		val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
		val parsed = withScheme.toHttpUrlOrNull() ?: throw IllegalArgumentException("Enter a valid server address.")
		return parsed.toString().trimEnd('/')
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
