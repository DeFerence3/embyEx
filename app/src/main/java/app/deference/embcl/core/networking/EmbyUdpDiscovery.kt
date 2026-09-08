package app.deference.embcl.core.networking

import android.content.Context
import android.net.wifi.WifiManager
import app.deference.embcl.domain.model.EmbyUdpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

class EmbyUdpDiscovery(
	private val context: Context,
) {
	private val json = Json { ignoreUnknownKeys = true }

	suspend fun discover(timeoutMs: Int = 3000): List<EmbyUdpServer> = withContext(Dispatchers.IO) {
		val servers = mutableListOf<EmbyUdpServer>()
		val seenIds = mutableSetOf<String>()
		val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
		val multicastLock = wifiManager?.createMulticastLock("EmbyUdpDiscovery")?.apply {
			setReferenceCounted(true)
			acquire()
		}

		var socket: DatagramSocket? = null
		try {
			socket = DatagramSocket().apply {
				broadcast = true
				soTimeout = timeoutMs
			}

			val messageBytes = "who is EmbyServer?".toByteArray(Charsets.UTF_8)
			val broadcastAddresses = getBroadcastAddresses()

			for (address in broadcastAddresses) {
				try {
					val packet = DatagramPacket(messageBytes, messageBytes.size, address, PORT)
					socket.send(packet)
				} catch (_: Exception) {}
			}

			val buffer = ByteArray(2048)
			val endTime = System.currentTimeMillis() + timeoutMs

			while (System.currentTimeMillis() < endTime) {
				val remainingTime = (endTime - System.currentTimeMillis()).toInt()
				if (remainingTime <= 0) break
				socket.soTimeout = remainingTime

				val responsePacket = DatagramPacket(buffer, buffer.size)
				try {
					socket.receive(responsePacket)
					val responseStr = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
					val parsed = runCatching { json.decodeFromString<EmbyUdpServer>(responseStr) }.getOrNull()
					if (parsed != null && seenIds.add(parsed.id)) {
						servers.add(parsed)
					}
				} catch (_: java.net.SocketTimeoutException) {
					break
				} catch (_: Exception) {}
			}
		} catch (_: Exception) {
		} finally {
			socket?.close()
			try {
				if (multicastLock?.isHeld == true) {
					multicastLock.release()
				}
			} catch (_: Exception) {}
		}

		servers
	}

	private fun getBroadcastAddresses(): List<InetAddress> {
		val broadcastList = mutableListOf<InetAddress>()
		try {
			val interfaces = NetworkInterface.getNetworkInterfaces() ?: return listOf(InetAddress.getByName("255.255.255.255"))
			while (interfaces.hasMoreElements()) {
				val networkInterface = interfaces.nextElement()
				if (networkInterface.isLoopback || !networkInterface.isUp) continue

				for (interfaceAddress in networkInterface.interfaceAddresses) {
					val broadcast = interfaceAddress.broadcast
					if (broadcast != null) {
						broadcastList.add(broadcast)
					}
				}
			}
		} catch (_: Exception) {}

		if (broadcastList.isEmpty()) {
			runCatching { broadcastList.add(InetAddress.getByName("255.255.255.255")) }
		}
		return broadcastList
	}

	companion object {
		const val PORT = 7359
	}
}
