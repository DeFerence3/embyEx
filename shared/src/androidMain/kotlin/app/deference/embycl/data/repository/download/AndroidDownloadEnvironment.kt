package app.deference.embycl.data.repository.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single(binds = [DownloadEnvironment::class])
class AndroidDownloadEnvironment(context: Context) : SessionDownloadEnvironment() {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(readNetwork())
    override val network = state.asStateFlow()
    private fun readNetwork(): DownloadNetwork {
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        return DownloadNetwork(capabilities != null, capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
    }
    init {
        // Do not require INTERNET or VALIDATED: a LAN-only Emby server is a valid destination.
        connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = readNetwork() }
            override fun onLost(network: Network) { state.value = readNetwork() }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { state.value = readNetwork() }
            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                state.value = if (blocked) DownloadNetwork(false) else readNetwork()
            }
        })
    }
}
