package app.deference.embycl.data.repository.download

import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single(binds = [DownloadEnvironment::class])
class IosDownloadEnvironment : SessionDownloadEnvironment() {
    override val network = MutableStateFlow(DownloadNetwork(false)).asStateFlow()
}
@Single(binds = [DownloadScheduler::class])
class IosDownloadScheduler : DownloadScheduler {
    override suspend fun requestExecution(userInitiated: Boolean) = DownloadScheduling.UNAVAILABLE
}
