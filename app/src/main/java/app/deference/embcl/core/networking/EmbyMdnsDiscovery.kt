package app.deference.embcl.core.networking

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import app.deference.embcl.domain.model.EmbyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

@Single
class EmbyMdnsDiscovery(
	private val context: Context,
) {
	
	companion object {
		
		private const val TAG = "EmbyMdnsDiscovery"
		const val SERVICE_TYPE = "_emby._tcp."
	}
	
	suspend fun discover(timeoutMs: Long = 4000L): List<EmbyServer> = withContext(Dispatchers.IO) {
		val discovered = ConcurrentHashMap<String, EmbyServer>()
		val resolvingServices = Collections.synchronizedSet(mutableSetOf<String>())
		val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return@withContext emptyList()
		val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
		val multicastLock = wifiManager?.createMulticastLock("EmbyMdnsDiscovery")?.apply {
			setReferenceCounted(true)
			acquire()
		}
		var discoveryListener: NsdManager.DiscoveryListener? = null
		try {
			discoveryListener = object : NsdManager.DiscoveryListener {
				override fun onDiscoveryStarted(serviceType: String) {
					Log.d(TAG, "mDNS discovery started for ")
				}
				
				override fun onServiceFound(serviceInfo: NsdServiceInfo) {
					Log.d(TAG, "mDNS service found: ")
					val serviceName = serviceInfo.serviceName
					if (! resolvingServices.add(serviceName)) return
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
						resolveServiceApi34(nsdManager, serviceInfo, serviceName, resolvingServices, discovered)
					} else {
						resolveServiceLegacy(nsdManager, serviceInfo, serviceName, resolvingServices, discovered)
					}
				}
				
				override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
				override fun onDiscoveryStopped(serviceType: String) {}
				override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
					Log.e(TAG, "mDNS start discovery failed: ")
				}
				
				override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
					Log.e(TAG, "mDNS stop discovery failed: ")
				}
			}
			
			nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
			delay(timeoutMs.milliseconds)
		} catch (e: Exception) {
			Log.e(TAG, "mDNS discovery failed", e)
		} finally {
			try {
				discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
			} catch (_: Exception) {
			}
			try {
				if (multicastLock?.isHeld == true) {
					multicastLock.release()
				}
			} catch (_: Exception) {
			}
		}
		
		discovered.values.toList()
	}
	
	@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
	private fun resolveServiceApi34(
		nsdManager: NsdManager,
		serviceInfo: NsdServiceInfo,
		serviceName: String,
		resolvingServices: MutableSet<String>,
		discovered: ConcurrentHashMap<String, EmbyServer>,
	) {
		val executor = Executors.newSingleThreadExecutor()
		val callback = object : NsdManager.ServiceInfoCallback {
			override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
				resolvingServices.remove(serviceName)
			}
			
			override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
				resolvingServices.remove(serviceName)
				try {
					nsdManager.unregisterServiceInfoCallback(this)
				} catch (_: Exception) {
				}
				val host = resolvedInfo.hostAddresses.firstOrNull()?.hostAddress
				val port = resolvedInfo.port
				handleResolved(resolvedInfo, host, port, discovered)
			}
			
			override fun onServiceLost() {
				resolvingServices.remove(serviceName)
			}
			
			override fun onServiceInfoCallbackUnregistered() {}
		}
		
		try {
			nsdManager.registerServiceInfoCallback(serviceInfo, executor, callback)
		} catch (e: Exception) {
			Log.e(TAG, "registerServiceInfoCallback failed", e)
			resolvingServices.remove(serviceName)
		}
	}
	
	@Suppress("DEPRECATION")
	private fun resolveServiceLegacy(
		nsdManager: NsdManager,
		serviceInfo: NsdServiceInfo,
		serviceName: String,
		resolvingServices: MutableSet<String>,
		discovered: ConcurrentHashMap<String, EmbyServer>,
	) {
		val listener = object : NsdManager.ResolveListener {
			override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
				resolvingServices.remove(serviceName)
			}
			
			override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
				resolvingServices.remove(serviceName)
				val host = resolvedInfo.host?.hostAddress
				val port = resolvedInfo.port
				handleResolved(resolvedInfo, host, port, discovered)
			}
		}
		
		try {
			nsdManager.resolveService(serviceInfo, listener)
		} catch (e: Exception) {
			Log.e(TAG, "resolveService failed", e)
			resolvingServices.remove(serviceName)
		}
	}
	
	private fun handleResolved(
		serviceInfo: NsdServiceInfo,
		host: String?,
		port: Int,
		discovered: ConcurrentHashMap<String, EmbyServer>,
	) {
		if (host != null && port > 0) {
			val formattedHost = if (host.contains(':') && ! host.startsWith("[")) "[System.Management.Automation.Internal.Host.InternalHost]" else host
			val serverAddress = "${host}:${port}"
			val id = serviceInfo.serviceName
			val server = EmbyServer(
				address = serverAddress,
				id = id,
				name = serviceInfo.serviceName,
			)
			discovered[serverAddress] = server
			Log.d(TAG, "Resolved mDNS Emby server: ")
		}
	}
}