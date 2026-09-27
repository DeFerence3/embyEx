package app.deference.embycl.core.networking

import app.deference.embycl.domain.model.EmbyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.net.NetworkInterface
import javax.jmdns.JmDNS

@Single
actual class EmbyMdnsDiscovery {
	actual suspend fun discover(timeoutMs: Long): List<EmbyServer> = withContext(Dispatchers.IO) {
		val addresses = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
			.filter { it.isUp && !it.isLoopback }
			.flatMap { it.inetAddresses.toList() }
			.filterIsInstance<java.net.Inet4Address>()
		coroutineScope {
			addresses.map { address -> async {
				try {
					JmDNS.create(address).use { dns ->
						dns.list("_emby._tcp.local.", timeoutMs).mapNotNull { service ->
							val host = service.inet4Addresses.firstOrNull()?.hostAddress ?: return@mapNotNull null
							EmbyServer(address = "$host:${service.port}", id = service.name, name = service.name)
						}
					}
				} catch (e: java.io.IOException) {
					emptyList()
				}
			} }.awaitAll().flatten().distinctBy { it.address }
		}
	}
}
