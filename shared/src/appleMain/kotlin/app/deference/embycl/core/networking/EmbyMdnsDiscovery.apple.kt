package app.deference.embycl.core.networking

import app.deference.embycl.domain.model.EmbyServer
import org.koin.core.annotation.Single

@Single
actual class EmbyMdnsDiscovery {
	
	actual suspend fun discover(timeoutMs: Long): List<EmbyServer> {
		TODO("Not yet implemented")
	}
}