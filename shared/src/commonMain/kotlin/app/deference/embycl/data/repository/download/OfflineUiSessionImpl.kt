package app.deference.embycl.data.repository.download

import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.download.DownloadConnection
import app.deference.embycl.domain.repository.OfflineUiSession
import org.koin.core.annotation.Single

@Single(binds = [OfflineUiSession::class])
class OfflineUiSessionImpl : OfflineUiSession {
    override val browsingLocalFiles get() = Session.browseLocalFiles.value
    override val account get() = Session.offlineAccount
    override val connected get() = Session.isConnected
    override fun address() = Session.preferences.getString("server-url") ?: ""
    override fun setConnected(value: Boolean) = Session.setConnected(value)
    override fun acceptAddress(address: String) = Session.updateServerUrl(address)
    override fun signOut() = Session.logout()
    override fun expire(connection: DownloadConnection) = Session.expireAuthentication(connection.accessToken)
}
