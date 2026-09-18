package app.deference.embcl.data.repository

import app.deference.embcl.core.networking.EmbyMdnsDiscovery
import app.deference.embcl.core.networking.EmbyUdpDiscovery
import app.deference.embcl.core.networking.dontIntercept
import app.deference.embcl.core.session.Server
import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.HttpScheme
import app.deference.embcl.core.utils.NetworkUtils.safeApiCall
import app.deference.embcl.core.utils.toUrl
import app.deference.embcl.domain.model.EmbyServer
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.model.PublicSystemInfo
import app.deference.embcl.domain.repository.ServerFinderRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.annotation.Single

@Single
class ServerFinderRepoImpl(
	private val udpDiscovery: EmbyUdpDiscovery,
	private val mdnsDiscovery: EmbyMdnsDiscovery,
	private val httpClient: HttpClient,
) : ServerFinderRepo {
	
	override suspend fun searchForLocallyRunningServers(): List<EmbyServer> {
		/** first check for emby's inbuilt udp discovery */
		val udpServers = udpDiscovery.discover()
		if (udpServers.isNotEmpty()) {
			return udpServers
		}
		/**
		 * if not found try mdns discovery, a 3rd party discovery service
		 * https:https://github.com/DeFerence3/emby-mDNS
		 * */
		return mdnsDiscovery.discover()
	}
	
	override suspend fun discoverServer(rawIp: String): EmbyServerDiscovery {
		val serverUrl = rawIp.toUrl().toHttpUrl()
		val deviceId = Session.deviceId
		return try {
			val publicInfo = safeApiCall {
				httpClient.get("/System/Info/Public") {
					dontIntercept(
						host = serverUrl.host,
						port = serverUrl.port
					)
				}.body<PublicSystemInfo>()
			}
			val users = safeApiCall {
				httpClient.get("/Users/Public") {
					dontIntercept(
						host = serverUrl.host,
						port = serverUrl.port
					)
				}.body<List<EmbyUser>>()
			}
			val server = Server(
				host = serverUrl.host,
				port = serverUrl.port,
				scheme = HttpScheme.fromHttpUrl(serverUrl)
			)
			EmbyServerDiscovery(server, publicInfo, users, deviceId)
		} catch (e: Exception) {
			throw e
		}
	}
}