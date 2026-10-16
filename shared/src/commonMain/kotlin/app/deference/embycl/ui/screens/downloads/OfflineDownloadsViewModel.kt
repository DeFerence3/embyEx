package app.deference.embycl.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.data.repository.download.DownloadController
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.domain.model.download.LocalPlaybackOutcome
import app.deference.embycl.domain.model.download.OfflineAsset
import app.deference.embycl.domain.model.download.OfflineAssetKind
import app.deference.embycl.domain.model.download.OfflineAssetState
import app.deference.embycl.domain.model.download.OfflinePlayback
import app.deference.embycl.domain.model.download.StoredDownload
import app.deference.embycl.domain.model.download.accountKey
import app.deference.embycl.domain.model.download.mediaKey
import app.deference.embycl.domain.repository.DownloadEnvironment
import app.deference.embycl.domain.repository.DownloadScheduling
import app.deference.embycl.domain.repository.OfflineRepository
import app.deference.embycl.domain.repository.OfflineServerConnector
import app.deference.embycl.domain.repository.OfflineServerResult
import app.deference.embycl.domain.repository.OfflineStorageException
import app.deference.embycl.domain.repository.OfflineStorageFailure
import app.deference.embycl.domain.repository.OfflineUiSession
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@KoinViewModel
class OfflineDownloadsViewModel(
    private val repository: OfflineRepository,
    private val storage: OfflineStorage,
    private val controller: DownloadController,
    private val environment: DownloadEnvironment,
    private val session: OfflineUiSession,
    private val connector: OfflineServerConnector,
    private val playback: OfflinePlaybackCoordinator,
) : ViewModel() {
    private val _state = MutableStateFlow(OfflineDownloadsUiState())
    val state = _state.asStateFlow()
    private val _events = Channel<DownloadsUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    private val reloads = MutableStateFlow(0)
    private var checking: Job? = null
    private var connecting: Job? = null
    private var discovery: Job? = null

    init {
        viewModelScope.launch {
            session.account.collectLatest { account ->
                if (account == null) return@collectLatest
                repository.observePendingSync(account.key).retryWhen { cause, _ ->
                    if (cause is CancellationException) throw cause
                    delay(30_000.milliseconds); true
                }.collect { pending ->
                    if (session.account.value?.key == account.key) _state.update { it.copy(pendingSync = pending) }
                }
            }
        }
        viewModelScope.launch {
            playback.request.collect { request -> _state.update { it.copy(playingId = request?.downloadId) } }
        }
        viewModelScope.launch {
            session.account.collectLatest { account ->
                checking?.cancel(); connecting?.cancel(); discovery?.cancel()
                _state.value = OfflineDownloadsUiState(account = account, playingId = playback.request.value?.downloadId, unmeteredOnly = _state.value.unmeteredOnly, connection = OfflineConnectionState(
                    account?.serverName ?: "Emby", account?.userName ?: "", session.address(),
                    if (environment.connection.value == null) OfflineConnectionStatus.SIGN_IN_REQUIRED else OfflineConnectionStatus.UNAVAILABLE))
                if (account == null) return@collectLatest
                session.setConnected(false)
                connect()
                reloads.collectLatest {
                    try {
                        controller.queue.recover()
                        repository.observeDownloads(account.key).flatMapLatest { rows ->
                            if (rows.isEmpty()) flowOf(emptyList()) else combine(rows.map { row ->
                                combine(repository.observeAssets(row.task.key), repository.observePlayback(row.task.key.mediaKey())) { assets, playback ->
                                    present(row, assets, playback)
                                }
                            }) { it.toList() }
                        }.collect { saved ->
                            if (session.account.value?.key == account.key) _state.update { it.copy(saved = saved,
                                downloads = it.downloads.copy(items = saved.map { it.item }, isLoading = false, loadFailed = false)) }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { if (session.account.value?.key == account.key) _state.update {
                        it.copy(saved = emptyList(), downloads = it.downloads.copy(items = emptyList(), isLoading = false, loadFailed = true))
                    } }
                }
            }
        }
        viewModelScope.launch {
            combine(session.connected, environment.connection) { connected, connection ->
                if (connection == null) OfflineConnectionStatus.SIGN_IN_REQUIRED
                else if (connected) OfflineConnectionStatus.CONNECTED else OfflineConnectionStatus.UNAVAILABLE
            }.collect { status -> _state.update { it.copy(connection = it.connection.copy(status = status), downloads = it.downloads.copy(isOffline = status != OfflineConnectionStatus.CONNECTED)) } }
        }
        viewModelScope.launch { environment.unmeteredOnly.collect { value -> _state.update { it.copy(unmeteredOnly = value) } } }
        viewModelScope.launch {
            var wasActive = false
            controller.queue.activity.collect { activity ->
                // The completion row can arrive before the writer releases its file lease.
                if (wasActive && activity == null) reload()
                wasActive = activity != null
            }
        }
        viewModelScope.launch {
            var previous = environment.connection.value
            environment.connection.collect { connection ->
                if (connection !== previous) {
                    checking?.cancel()
                    _state.update { it.copy(choice = null, checkingItem = null) }
                    previous = connection
                }
            }
        }
    }

    private suspend fun present(row: StoredDownload, assets: List<OfflineAsset>, playback: OfflinePlayback?): SavedDownloadUi {
        val previous = _state.value.saved.firstOrNull { it.record.id == row.id }
        var checking = false
        val available = if (row.task.status != DownloadStatus.COMPLETED) false else try { storage.localMedia(row.task.key) != null }
        catch (e: OfflineStorageException) {
            checking = e.reason == OfflineStorageFailure.BUSY && previous?.item?.fileAvailable != true
            e.reason == OfflineStorageFailure.BUSY && previous?.item?.fileAvailable == true
        }
        suspend fun asset(id: String, prior: String?): String? {
            if (assets.none { it.id == id && it.state == OfflineAssetState.READY }) return null
            return try { storage.localAsset(row.task.key, id)?.absolutePath?.let(::offlineImageUri) }
            catch (e: OfflineStorageException) { if (e.reason == OfflineStorageFailure.BUSY) prior else null }
        }
        val poster = asset("poster", previous?.item?.localArtwork)
        val position = playback?.positionTicks ?: 0
        val metadata = row.media.item
        val checkedAssets = assets.map { asset ->
            if (asset.kind != OfflineAssetKind.SUBTITLE || asset.state != OfflineAssetState.READY) asset
            else try {
                if (storage.localAsset(row.task.key, asset.id) != null) asset else asset.copy(state = OfflineAssetState.MISSING)
            } catch (e: OfflineStorageException) {
                if (e.reason == OfflineStorageFailure.BUSY && previous?.assetStates?.any { it.id == asset.id && it.state == OfflineAssetState.READY } == true) asset
                else asset.copy(state = OfflineAssetState.MISSING)
            }
        }
        return SavedDownloadUi(row, DownloadListItem(row.id, metadata.name,
            listOfNotNull(metadata.seriesName, metadata.subtitle(), row.source.name).joinToString(" · "), row.task, poster,
            available, if (position > 0) "Resume at ${position / 10_000_000 / 60} min" else null, checking),
            asset("backdrop", previous?.backdrop), asset("logo", previous?.logo), position, checkedAssets)
    }

    fun reload() { reloads.value++ }
    fun openSync() { _state.update { it.copy(syncPanelOpen = true) } }

    fun openConnection() { _state.update { it.copy(panelOpen = true) } }
    private fun message(text: String) { _state.update { it.copy(message = text) } }

    fun onAction(action: DownloadsAction) {
        when (action) {
            is DownloadsAction.QueryChanged -> _state.update { it.copy(downloads = it.downloads.copy(query = action.query)) }
            is DownloadsAction.FilterChanged -> _state.update { it.copy(downloads = it.downloads.copy(filter = action.filter)) }
            is DownloadsAction.OpenDetails -> if (current(action.id) != null) _events.trySend(DownloadsUiEvent.Details(action.id))
            is DownloadsAction.Play -> play(action.id)
            is DownloadsAction.Pause -> control(action.id) { controller.queue.pause(it.task.key) }
            is DownloadsAction.Cancel -> control(action.id) { controller.queue.cancel(it.task.key) }
            is DownloadsAction.Remove -> control(action.id) { controller.queue.remove(it.task.key) }
            is DownloadsAction.Resume -> retry(action.id)
            is DownloadsAction.Retry -> retry(action.id)
            DownloadsAction.Reload -> reload()
            DownloadsAction.Reconnect -> openConnection()
            DownloadsAction.SignIn -> _events.trySend(DownloadsUiEvent.SignIn)
            DownloadsAction.BrowseLibrary -> _events.trySend(DownloadsUiEvent.Library)
        }
    }
    private fun current(id: String) = _state.value.saved.firstOrNull { it.record.id == id && it.record.task.key.accountKey() == session.account.value?.key }?.record
    private fun play(id: String) {
        val row = current(id) ?: return
        if (id in _state.value.busyIds) return
        viewModelScope.launch {
            val outcome = playback.play(row.task.key, row.id)
            if (session.account.value?.key == row.task.key.accountKey()) {
                val error = when (outcome) {
                    LocalPlaybackOutcome.SAVED -> null
                    LocalPlaybackOutcome.NO_PROGRESS -> "The player returned no usable position. Your previous progress was kept."
                    LocalPlaybackOutcome.CONFLICT -> "Newer watch progress was kept."
                    LocalPlaybackOutcome.UNAVAILABLE -> "This saved file is unavailable. Refresh Downloads and check its status."
                    LocalPlaybackOutcome.BUSY -> "This file or the player is busy. Close the current playback and try again."
                    LocalPlaybackOutcome.STORAGE_FAILED -> "Couldn't access the download or save progress. Check storage and try again."
                    LocalPlaybackOutcome.PLAYER_MISSING -> "Player not found. Install mpvEx on Android, or choose mpv in desktop Settings."
                    LocalPlaybackOutcome.PLAYER_FAILED -> "The player couldn't open this download. Check the player and its support for this media format."
                    LocalPlaybackOutcome.POSITION_UNSUPPORTED -> "This saved position exceeds mpvEx's supported resume range. Your progress was kept."
                    LocalPlaybackOutcome.UNSUPPORTED -> "Local playback isn't supported on this device."
                }
                error?.let(::message)
            }
            reload()
        }
    }
    private fun control(id: String, operation: suspend (StoredDownload) -> Unit) {
        val row = current(id) ?: return
        if (playback.request.value?.downloadId == id) { message("Close the player before changing this download."); return }
        if (id in _state.value.busyIds) return
        _state.update { it.copy(busyIds = it.busyIds + id) }
        viewModelScope.launch {
            try { operation(row) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (session.account.value?.key == row.task.key.accountKey()) message("Couldn't update this download. Check storage and try again.") }
            finally { _state.update { it.copy(busyIds = it.busyIds - id) }; reload() }
        }
    }
    private fun retry(id: String) = control(id) { row ->
        if (environment.connection.value == null) { openConnection(); return@control }
        scheduled(controller.retry(row.task.key))
    }
    
    fun setUnmeteredOnly(value: Boolean) {
        viewModelScope.launch {
            try { scheduled(controller.setUnmeteredOnly(value)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("Couldn't save the network preference.") }
        }
    }
    private fun scheduled(result: DownloadScheduling) {
        when (result) {
            DownloadScheduling.SCHEDULED -> Unit
            DownloadScheduling.REQUIRES_USER_ACTION -> message("Your download is saved in the queue. Keep EmbyEx open and tap Resume queue to start it.")
            DownloadScheduling.UNAVAILABLE -> message("Couldn't start background downloads. Your queue is saved; try Resume queue again.")
        }
    }

    private fun connect() {
        val account = session.account.value ?: return
        val address = _state.value.connection.address.trim()
        val connection = environment.connection.value
        connecting?.cancel()
        _state.update { it.copy(connection = it.connection.copy(isConnecting = true, error = null)) }
        connecting = viewModelScope.launch {
            try {
                val result = connector.verify(account.key, address, connection)
                if (session.account.value?.key != account.key || environment.connection.value !== connection) return@launch
                when (result) {
                    OfflineServerResult.CONNECTED -> { session.acceptAddress(address); session.setConnected(true) }
                    OfflineServerResult.SIGN_IN_REQUIRED -> { session.acceptAddress(address); connection?.let(session::expire); session.setConnected(false) }
                    OfflineServerResult.WRONG_SERVER -> _state.update { it.copy(connection = it.connection.copy(error = "This address belongs to a different server. Saved media stays with the original account.")) }
                    OfflineServerResult.UNAVAILABLE -> { session.setConnected(false); _state.update { it.copy(connection = it.connection.copy(error = "Couldn't reach this server. Check its address and try again.")) } }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("Couldn't connect to this server.") }
            finally { if (session.account.value?.key == account.key) _state.update { it.copy(connection = it.connection.copy(isConnecting = false)) } }
        }
    }
}

/** File URI supports Windows drive letters, spaces and non-ASCII paths in Coil's local fetcher. */
private fun offlineImageUri(path: String): String {
    val normalized = path.replace('\\', '/').let { if (it.startsWith('/')) it else "/$it" }
    return "file://" + normalized.encodeURLPath()
}
