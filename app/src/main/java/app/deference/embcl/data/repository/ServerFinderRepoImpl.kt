package app.deference.embcl.data.repository

import app.deference.embcl.core.networking.ApiResponseHandler.safeApiCall
import app.deference.embcl.core.networking.EmbyMdnsDiscovery
import app.deference.embcl.core.networking.EmbyUdpDiscovery
import app.deference.embcl.core.session.Session
import app.deference.embcl.core.utils.Log
import app.deference.embcl.core.utils.toUrl
import app.deference.embcl.domain.model.EmbyServer
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.model.PublicSystemInfo
import app.deference.embcl.domain.repository.ServerFinderRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.koin.core.annotation.Single

@Single
class ServerFinderRepoImpl(
	private val udpDiscovery: EmbyUdpDiscovery,
	private val mdnsDiscovery: EmbyMdnsDiscovery,
	private val httpClient: HttpClient,
) : ServerFinderRepo {
	
	override suspend fun searchForLocallyRunningServers(): List<EmbyServer> {
		//first check for emby's inbuilt udp discovery
		val udpServers = udpDiscovery.discover()
		if (udpServers.isNotEmpty()) {
			return udpServers
		}
		//if not found try mdns discovery, a 3rd party discovery service
		// https://github.com/DeFerence3/emby-mDNS
		return mdnsDiscovery.discover()
	}
	
	override suspend fun discoverServer(server: String): EmbyServerDiscovery {
		val serverUrl = server.toUrl()
		val deviceId = Session.deviceId
		Log.i("ServerFinderRepoImpl") { "ServerUrl---> $serverUrl" }
		return try {
			val publicInfo = safeApiCall {
				httpClient.get("/System/Info/Public") {
					url {
						host = serverUrl
					}
				}.body<PublicSystemInfo>()
//				api.publicSystemInfo()
			}
			val users = safeApiCall {
				httpClient.get("/Users/Public").body<List<EmbyUser>>()
//				api.publicUsers()
			}
			EmbyServerDiscovery(serverUrl, publicInfo, users, deviceId)
		} catch (e: Exception) {
			throw e
		}
	}
}