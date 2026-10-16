package app.deference.embycl.domain.repository

import app.deference.embycl.domain.model.download.*
import kotlinx.coroutines.flow.StateFlow

interface OfflineUiSession {
    val browsingLocalFiles: Boolean get() = false
    val account: StateFlow<OfflineAccount?>
    val connected: StateFlow<Boolean>
    fun address(): String
    fun setConnected(value: Boolean)
    fun acceptAddress(address: String)
    fun signOut()
    fun expire(connection: DownloadConnection)
}

enum class OfflineServerResult { CONNECTED, SIGN_IN_REQUIRED, WRONG_SERVER, UNAVAILABLE }
interface OfflineServerConnector {
    suspend fun verify(account: OfflineAccountKey, address: String, connection: DownloadConnection?): OfflineServerResult
}
