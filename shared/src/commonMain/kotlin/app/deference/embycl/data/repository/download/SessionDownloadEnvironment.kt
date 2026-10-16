package app.deference.embycl.data.repository.download

import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.repository.DownloadEnvironment

abstract class SessionDownloadEnvironment : DownloadEnvironment {
    final override val connection get() = Session.downloadConnection
    final override val unmeteredOnly get() = Session.preferences.observeBoolean("downloads_unmetered_only")
    final override suspend fun setUnmeteredOnly(value: Boolean) {
        Session.preferences.setBoolean("downloads_unmetered_only", value)
    }
}
