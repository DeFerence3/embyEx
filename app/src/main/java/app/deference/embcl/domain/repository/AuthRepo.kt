package app.deference.embcl.domain.repository

import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbySession
import app.deference.embcl.domain.model.EmbyUdpServer
import app.deference.embcl.domain.model.EmbyUser
import kotlinx.serialization.SerialInfo
import org.koin.core.annotation.Single

@Single
interface AuthRepo {
	suspend fun authenticate(server: String, username: String, password: String): EmbySession
	suspend fun discoverServer(server: String): EmbyServerDiscovery
	suspend fun discoverLocalServers(): List<EmbyUdpServer>
	suspend fun authenticate(discovery: EmbyServerDiscovery, username: String, password: String): EmbySession
	suspend fun authenticate(discovery: EmbyServerDiscovery, user: EmbyUser, password: String): EmbySession
}