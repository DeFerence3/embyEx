package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.EmbyServer
import app.deference.embycl.domain.model.EmbyServerDiscovery
import org.koin.core.annotation.Single

@Single
interface ServerFinderRepo {
	suspend fun discoverServer(rawIp: String): EmbyServerDiscovery
	suspend fun searchForLocallyRunningServers(): List<EmbyServer>
}