package app.deference.embycl.data.repository

import app.deference.embycl.core.session.Session
import app.deference.embycl.data.local.database.OfflineStorage
import app.deference.embycl.data.repository.download.OfflinePlaybackCoordinator
import app.deference.embycl.domain.model.*
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.annotation.Single

/** The catalogue boundary: screens consume the same models regardless of their source. */
@OptIn(ExperimentalCoroutinesApi::class)
@Single(binds = [EmbyRepository::class])
class EmbyRepositoryImpl(
    private val remote: RemoteEmbyRepository,
    private val offline: OfflineRepository,
    private val storage: OfflineStorage,
    private val playback: OfflinePlaybackCoordinator,
) : EmbyRepository by remote {
    private val accounts = combine(Session.browseLocalFiles, Session.offlineAccount, offline.observeAccounts()) { local, selected, saved ->
        if (local) saved else listOfNotNull(selected)
    }.distinctUntilChanged()

    override val changes: Flow<Unit> = combine(
        accounts.flatMapLatest { selected ->
            if (selected.isEmpty()) flowOf(Unit) else combine(selected.map { account ->
                offline.observeDownloads(account.key).map { rows -> rows.filter { it.isLocallyAvailable } }.distinctUntilChanged().flatMapLatest { rows ->
                    if (rows.isEmpty()) flowOf(Unit) else combine(rows.map { row ->
                        combine(offline.observePlayback(row.task.key.mediaKey()), offline.observeAssets(row.task.key)) { _, _ -> Unit }
                    }) { Unit }
                }
            }) { Unit }
        }, Session.browseLocalFiles, Session.downloadConnection, Session.isConnected,
    ) { _, _, _, _ -> }

    private suspend fun rows(): List<StoredDownload> = accounts.first().flatMap { account ->
        offline.observeDownloads(account.key).first()
    }.filter { it.isLocallyAvailable }.sortedByDescending { it.createdAt }

    private fun itemId(row: StoredDownload): String = if (Session.browseLocalFiles.value) "local-media:${row.id}" else row.task.key.itemId
    private fun folderId(row: StoredDownload, id: String): String {
        if (!Session.browseLocalFiles.value) return id
        val key = row.task.key
        return "local-folder:${key.serverId.length}:${key.serverId}${key.userId.length}:${key.userId}$id"
    }

    private suspend fun available(row: StoredDownload): Boolean = try {
        storage.localMedia(row.task.key) != null
    } catch (e: CancellationException) { throw e }
    catch (e: OfflineStorageException) {
        // An active player owns the file lease; it is still a valid local copy.
        e.reason == OfflineStorageFailure.BUSY && playback.request.value?.downloadId == row.id
    }

    private suspend fun decorate(item: EmbyItem, row: StoredDownload?, local: Boolean): EmbyItem {
        if (row == null || !available(row)) return item.copy(useServerArtwork = !local, isAvailableOffline = false)
        val images = buildMap {
            for ((kind, type) in listOf("poster" to "Primary", "backdrop" to "Backdrop", "logo" to "Logo")) {
                val file = try { storage.localAsset(row.task.key, kind) } catch (e: CancellationException) { throw e } catch (_: OfflineStorageException) { null }
                file?.let { put(type, "file://" + (if (it.absolutePath.startsWith("/")) "" else "/") + it.absolutePath.replace('\\', '/').encodeURLPath()) }
            }
        }
        val progress = offline.observePlayback(row.task.key.mediaKey()).first()
        val userData = if (progress != null && (local || progress.revision > progress.acknowledgedRevision))
            (item.userData ?: EmbyUserData()).copy(playbackPositionTicks = progress.positionTicks, played = progress.played)
        else item.userData
        return item.copy(isAvailableOffline = true, artwork = images, useServerArtwork = !local, userData = userData,
            mediaStreams = if (local) row.source.streams else item.mediaStreams)
    }

    private suspend fun mark(items: List<EmbyItem>): List<EmbyItem> {
        val saved = rows().groupBy { it.task.key.itemId }
        return items.map { item -> decorate(item, saved[item.id]?.firstOrNull { available(it) }, false) }
    }

    private suspend fun catalogue(): List<EmbyItem> = rows().filter { available(it) }
        .distinctBy { it.task.key.mediaKey() }.map { row ->
            val item = row.media.item
            decorate(item.copy(id = itemId(row),
                parentId = item.parentId?.let { folderId(row, it) },
                seriesId = item.seriesId?.let { folderId(row, it) },
                seasonId = item.seasonId?.let { folderId(row, it) }), row, true)
        }

    private fun libraries(items: List<EmbyItem>): List<EmbyItem> = listOf(
        Triple("local-library:movies", "Movies", "movies"),
        Triple("local-library:tv", "TV shows", "tvshows"),
        Triple("local-library:videos", "Videos", "homevideos"),
    ).filter { (id, _, _) -> items.any { libraryId(it) == id } }
        .map { (id, name, collection) -> EmbyItem(id, name, type = "CollectionFolder", isFolder = true, collectionType = collection, useServerArtwork = false) }

    private fun libraryId(item: EmbyItem) = when (item.type) {
        "Movie" -> "local-library:movies"
        "Episode" -> "local-library:tv"
        else -> "local-library:videos"
    }

    private fun children(parentId: String, items: List<EmbyItem>): List<EmbyItem> {
        val matches = items.filter { libraryId(it) == parentId || it.parentId == parentId || it.seriesId == parentId || it.seasonId == parentId }
        return matches.map { item ->
            when {
                parentId == "local-library:tv" && item.seriesId != null -> item.copy(id = item.seriesId, name = item.seriesName ?: "TV show", type = "Series", isFolder = true)
                parentId == item.seriesId && item.seasonId != null -> item.copy(id = item.seasonId, name = item.seasonName ?: "Season ${item.parentIndexNumber ?: 1}", type = "Season", isFolder = true)
                else -> item
            }
        }.distinctBy { it.id }.sortedWith(compareBy<EmbyItem> { it.parentIndexNumber ?: 0 }.thenBy { it.indexNumber ?: 0 }.thenBy { it.name.lowercase() })
    }

    private suspend fun <T> read(local: suspend () -> T, server: suspend () -> T): T {
        if (Session.browseLocalFiles.value || Session.downloadConnection.value == null || !Session.isConnected.value) return local()
        val account = Session.offlineAccount.value?.key
        return try {
            val result = server()
            currentCoroutineContext().ensureActive()
            check(account == Session.offlineAccount.value?.key) { "Account changed" }
            result
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            if (account != Session.offlineAccount.value?.key) throw e
            if (rows().isEmpty()) throw e
            local()
        }
    }

    override suspend fun home(): EmbyHome = read(local = {
        val items = catalogue()
        EmbyHome(libraries(items), items.filter { (it.userData?.playbackPositionTicks ?: 0) > 0 && it.userData?.played != true }, items)
    }) {
        val home = remote.home()
        home.copy(resume = mark(home.resume), latest = mark(home.latest))
    }
    override suspend fun libraries(): List<EmbyItem> = read({ libraries(catalogue()) }) { remote.libraries() }
    override suspend fun items(parentId: String, startIndex: Int): EmbyItemsResult {
        suspend fun local(): EmbyItemsResult {
            val items = children(parentId, catalogue())
            return EmbyItemsResult(items.drop(startIndex.coerceAtLeast(0)).take(100), items.size)
        }
        if (parentId.startsWith("local-")) return local()
        return read({ local() }) { remote.items(parentId, startIndex).let { it.copy(items = mark(it.items)) } }
    }
    override suspend fun search(term: String): List<EmbyItem> {
        if (term.isBlank()) return emptyList()
        return read({ catalogue().filter { it.name.contains(term.trim(), true) || it.seriesName?.contains(term.trim(), true) == true } }) { mark(remote.search(term)) }
    }
    override suspend fun item(id: String): EmbyItem {
        suspend fun local() = catalogue().firstOrNull { it.id == id } ?: error("This item is not available on this device.")
        if (id.startsWith("local-media:")) return local()
        return read({ local() }) { mark(listOf(remote.item(id))).single() }
    }
    override suspend fun play(item: EmbyItem): EmbyPlaybackRequest? {
        val row = rows().firstOrNull { itemId(it) == item.id && available(it) }
        if (row == null) {
            check(!Session.browseLocalFiles.value && Session.downloadConnection.value != null && Session.isConnected.value) { "This media is no longer available on this device." }
            return remote.play(item)
        }
        when (playback.play(row.task.key, row.id)) {
            LocalPlaybackOutcome.SAVED, LocalPlaybackOutcome.NO_PROGRESS, LocalPlaybackOutcome.CONFLICT -> Unit
            else -> error("Could not play the saved copy. Check the file and your player settings.")
        }
        return null
    }
    override suspend fun playFromServer(item: EmbyItem): EmbyPlaybackRequest? {
        check(!Session.browseLocalFiles.value && Session.downloadConnection.value != null && Session.isConnected.value) { "Connect to your server to play from it." }
        return remote.play(item)
    }
    override suspend fun serverInfo(): ServerDetails {
        check(!Session.browseLocalFiles.value && Session.downloadConnection.value != null) { "No server is connected." }
        return remote.serverInfo()
    }
    override fun userImageUrl(): String = if (Session.browseLocalFiles.value || Session.downloadConnection.value == null) "" else remote.userImageUrl()
    override suspend fun toggleFavorite(itemId: String, isFavorite: Boolean) {
        check(!Session.browseLocalFiles.value && Session.downloadConnection.value != null && Session.isConnected.value) { "Connect to your server to change favorites." }
        remote.toggleFavorite(itemId, isFavorite)
    }
    override suspend fun togglePlayed(itemId: String, isPlayed: Boolean) {
        val row = rows().firstOrNull { itemId(it) == itemId }
        if (row != null) offline.savePlayback(row.task.key.mediaKey(), 0, isPlayed)
        else {
            check(!Session.browseLocalFiles.value && Session.downloadConnection.value != null && Session.isConnected.value) { "This item is not available." }
            remote.togglePlayed(itemId, isPlayed)
        }
    }
}
