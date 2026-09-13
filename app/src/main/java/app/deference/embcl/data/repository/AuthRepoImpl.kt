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
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.EmbyUdpServer
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.AuthRepo
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.core.annotation.Single

@Single
class AuthRepoImpl(
	private val api: EmbyApiService,
	private val sessionStore: EmbySessionStore,
	private val hostSelectionInterceptor: HostSelectionInterceptor,
	private val udpDiscovery: EmbyUdpDiscovery,
	private val mdnsDiscovery: EmbyMdnsDiscovery,
) : AuthRepo {
	
	override suspend fun authenticate(server: String, username: String, password: String): EmbySession =
		authenticate(discoverServer(server), username, password)
	
	override suspend fun discoverLocalServers(): List<EmbyUdpServer> {
		val udpServers = udpDiscovery.discover()
		if (udpServers.isNotEmpty()) {
			return udpServers
		}
		return mdnsDiscovery.discover()
	}
	
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
}