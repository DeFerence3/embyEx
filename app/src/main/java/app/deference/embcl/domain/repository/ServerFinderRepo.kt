package app.deference.embcl.domain.repository

import app.deference.embcl.domain.model.EmbyServer
import app.deference.embcl.domain.model.EmbyServerDiscovery
import org.koin.core.annotation.Single

@Single
interface ServerFinderRepo {
	
	suspend fun discoverServer(server: String): EmbyServerDiscovery
	suspend fun searchForLocallyRunningServers(): List<EmbyServer>
}