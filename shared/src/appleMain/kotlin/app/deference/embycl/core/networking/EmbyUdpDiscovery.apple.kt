package app.deference.embycl.core.networking

import app.deference.embycl.domain.model.EmbyServer
import org.koin.core.annotation.Single

@Single
actual class EmbyUdpDiscovery {
	
	actual suspend fun discover(timeoutMs: Int): List<EmbyServer> {
		TODO("Not yet implemented")
	}
}