package app.deference.embycl.data.local.database

import app.deference.embycl.domain.model.download.*
import app.deference.embycl.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single

data class OfflineRecoveryIssue(val downloadId: String, val reason: OfflineStorageFailure)

/** File/database ordering only. C8 invokes recovery and owns scheduling; C7 supplies bytes. */
@Single
class OfflineStorage(private val repository: OfflineRepository, private val files: OfflineFileStore) {
    /** Keep the lease until the external player returns; removal/recovery must not race playback. */
    suspend fun <T> withPlaybackFiles(key: DownloadKey, downloadId: String,
        block: suspend (StoredDownload, OfflineFile, List<LocalPlaybackSubtitle>) -> T,
    ): T? = files.withDownload(downloadId) { directory ->
        val row = repository.getDownload(key)?.takeIf { it.id == downloadId && it.task.status == DownloadStatus.COMPLETED }
            ?: return@withDownload null
        if (directory.removalPending()) return@withDownload null
        val media = directory.findFile(mediaPath(row), row.task.progress.downloadedBytes) ?: return@withDownload null
        val assets = repository.observeAssets(key).first()
        val subtitles = row.source.subtitles.filter { it.availability == SubtitleAvailability.SEPARATE_FILE }.mapNotNull { subtitle ->
            val asset = assets.firstOrNull { it.kind == OfflineAssetKind.SUBTITLE && it.streamIndex == subtitle.stream.index && it.state == OfflineAssetState.READY }
                ?: return@mapNotNull null
            val file = try { assetPath(row, asset)?.let { directory.findFile(it, asset.sizeBytes) } }
                catch (_: OfflineStorageException) { null } // Optional tracks must not prevent playing the original.
            file?.let { LocalPlaybackSubtitle(it, subtitle.stream.isDefault || subtitle.stream.isForced) }
        }
        block(row, media, subtitles)
    }

    private suspend fun current(key: DownloadKey, version: DownloadVersion): StoredDownload? =
        repository.getDownload(key)?.takeIf { it.version == version }

    suspend fun finalizeMedia(key: DownloadKey, expected: DownloadVersion): StoredDownload? =
        files.withDownload(expected.downloadId) { directory ->
            val row = current(key, expected) ?: return@withDownload null
            finalize(row, directory)
        }

    private suspend fun finalize(row: StoredDownload, directory: DownloadFiles): StoredDownload? {
        check(row.task.status == DownloadStatus.VERIFYING)
        val file = directory.finalizeMedia(row.source.container, row.task.progress.downloadedBytes)
        return repository.updateDownload(row.task.key, row.version) {
            it.change(task = it.task.completeAfterVerification(), relativeMediaPath = file.relativePath,
                localMediaState = LocalMediaState.AVAILABLE)
        }
    }

    /** Retained bytes are not removed on sign-out. Only an explicit removal invokes this. */
    suspend fun remove(key: DownloadKey, expected: DownloadVersion): Boolean = files.withDownload(expected.downloadId) { directory ->
        val row = current(key, expected) ?: return@withDownload false
        directory.beginRemoval()
        remove(row, directory)
    }

    private suspend fun remove(row: StoredDownload, directory: DownloadFiles): Boolean {
        directory.deletePayloads()
        if (!repository.removeDownloadAfterFileDeletion(row.task.key, row.version)) return false
        directory.finishRemoval()
        return true
    }

    /** A persisted AVAILABLE flag alone is not enough to launch a player. */
    suspend fun localMedia(key: DownloadKey): OfflineFile? {
        val before = repository.getDownload(key) ?: return null
        return files.withDownload(before.id) { directory ->
            val row = repository.getDownload(key)?.takeIf { it.id == before.id } ?: return@withDownload null
            if (row.task.status != DownloadStatus.COMPLETED || directory.removalPending()) return@withDownload null
            val path = mediaPath(row)
            directory.findFile(path, row.task.progress.downloadedBytes)
        }
    }

    private fun mediaPath(row: StoredDownload): String {
        val expected = "${row.id}/media.${row.source.container.lowercase()}"
        if (row.relativeMediaPath != expected) throw OfflineStorageException(OfflineStorageFailure.INVALID_PATH)
        return expected
    }

    private fun assetPath(row: StoredDownload, asset: OfflineAsset): String? {
        val path = asset.relativePath ?: return null
        val target = try { OfflineAssetTarget(asset.kind, path.substringAfterLast('.'), asset.streamIndex) }
        catch (_: IllegalArgumentException) { throw OfflineStorageException(OfflineStorageFailure.INVALID_PATH) }
        if (asset.id != target.id || path != "${row.id}/${target.fileName}" ||
            (asset.kind == OfflineAssetKind.SUBTITLE && row.source.subtitles.none {
                it.availability == SubtitleAvailability.SEPARATE_FILE && it.stream.index == target.streamIndex && it.format == target.extension
            })) throw OfflineStorageException(OfflineStorageFailure.INVALID_PATH)
        return path
    }

    suspend fun localAsset(key: DownloadKey, assetId: String): OfflineFile? {
        val before = repository.getDownload(key) ?: return null
        return files.withDownload(before.id) { directory ->
            val row = repository.getDownload(key)?.takeIf { it.id == before.id } ?: return@withDownload null
            if (row.task.status == DownloadStatus.CANCELED || directory.removalPending()) return@withDownload null
            val asset = repository.observeAssets(key).first().firstOrNull { it.id == assetId && it.state == OfflineAssetState.READY }
                ?: return@withDownload null
            assetPath(row, asset)?.let { directory.findFile(it, asset.sizeBytes) }
        }
    }

    /** The producer returns only at successful EOF; network protocol validation belongs to C7. */
    suspend fun saveAsset(
        key: DownloadKey, expected: DownloadVersion, target: OfflineAssetTarget,
        expectedBytes: Long?, producer: suspend (OfflineFileWriter) -> Unit,
    ): OfflineAsset? = files.withDownload(expected.downloadId) { directory ->
        val row = current(key, expected) ?: return@withDownload null
        check(row.task.status != DownloadStatus.CANCELED)
        if (directory.removalPending()) throw OfflineStorageException(OfflineStorageFailure.REMOVAL_PENDING)
        require(expectedBytes == null || expectedBytes in 1..target.maxBytes)
        if (target.kind == OfflineAssetKind.SUBTITLE) {
            require(row.source.subtitles.any { it.availability == SubtitleAvailability.SEPARATE_FILE &&
                it.stream.index == target.streamIndex && it.format == target.extension }) { "Subtitle is not a supported sidecar for this source." }
        }
        val existing = repository.observeAssets(key).first().firstOrNull { it.id == target.id }
        if (existing?.state == OfflineAssetState.READY &&
            assetPath(row, existing)?.let { directory.findFile(it, existing.sizeBytes) } != null) return@withDownload existing

        val pending = OfflineAsset(target.id, target.kind, OfflineAssetState.PENDING,
            "${row.id}/${target.fileName}", expectedBytes, target.streamIndex)
        if (!repository.saveAsset(key, row.version, pending)) return@withDownload null
        val saved = try {
            directory.discardAsset(target)
            directory.writeAsset(target, expectedBytes, producer)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // Optional asset failure never changes media completion or availability.
            repository.saveAsset(key, row.version, pending.copy(state = OfflineAssetState.FAILED))
            throw e
        }
        val asset = pending.copy(state = OfflineAssetState.READY, sizeBytes = saved.sizeBytes)
        if (repository.saveAsset(key, row.version, asset)) asset else null
    }

    /** Returns null if absent or removed. A storage exception leaves availability untouched. */
    suspend fun recover(key: DownloadKey): StoredDownload? {
        val before = repository.getDownload(key) ?: return null
        return files.withDownload(before.id) { directory ->
            var row = repository.getDownload(key)?.takeIf { it.id == before.id } ?: return@withDownload null
            if (directory.removalPending()) {
                remove(row, directory)
                return@withDownload repository.getDownload(key)
            }
            when (row.task.status) {
                DownloadStatus.VERIFYING -> {
                    row = try { finalize(row, directory) ?: return@withDownload repository.getDownload(key) }
                    catch (e: OfflineStorageException) {
                        if (e.reason != OfflineStorageFailure.SIZE_MISMATCH) throw e
                        repository.updateDownload(key, row.version) {
                            it.change(task = it.task.fail(DownloadFailureReason.VERIFICATION_FAILED))
                        } ?: return@withDownload repository.getDownload(key)
                    }
                }
                DownloadStatus.COMPLETED -> {
                    val state = if (directory.findFile(mediaPath(row), row.task.progress.downloadedBytes) == null)
                        LocalMediaState.MISSING else LocalMediaState.AVAILABLE
                    if (row.localMediaState != state) row = repository.updateDownload(key, row.version) {
                        it.change(localMediaState = state)
                    } ?: return@withDownload repository.getDownload(key)
                }
                else -> {
                    if (row.task.status == DownloadStatus.CANCELED) directory.deletePayloads()
                    var bytes = directory.partialSize()
                    if (row.task.progress.totalBytes?.let { bytes > it } == true) {
                        directory.discardMedia(row.source.container)
                        bytes = 0
                    }
                    val task = row.task.reconcilePartialBytes(bytes)
                    if (task != row.task) row = repository.updateDownload(key, row.version) {
                        it.change(task = task)
                    } ?: return@withDownload repository.getDownload(key)
                }
            }
            recoverAssets(row, directory)
            row
        }
    }

    private suspend fun recoverAssets(row: StoredDownload, directory: DownloadFiles) {
        directory.discardAssetPartials()
        repository.observeAssets(row.task.key).first().forEach { asset ->
            val file = assetPath(row, asset)?.let { directory.findFile(it, asset.sizeBytes) }
            val state = when {
                file != null -> OfflineAssetState.READY
                asset.state == OfflineAssetState.PENDING || asset.state == OfflineAssetState.FAILED -> OfflineAssetState.FAILED
                else -> OfflineAssetState.MISSING
            }
            val recovered = asset.copy(state = state, sizeBytes = file?.sizeBytes ?: asset.sizeBytes)
            if (recovered != asset) repository.saveAsset(row.task.key, row.version, recovered)
        }
    }

    /** Call before scheduling transfers. Busy jobs are reported and left for a later retry. */
    suspend fun recoverAll(): List<OfflineRecoveryIssue> {
        val issues = mutableListOf<OfflineRecoveryIssue>()
        suspend fun recorded() = repository.observeAccounts().first().flatMap {
            repository.observeDownloads(it.key).first()
        }
        recorded().forEach { row ->
            try { recover(row.task.key) } catch (e: OfflineStorageException) { issues += OfflineRecoveryIssue(row.id, e.reason) }
        }
        // Crash after the DB delete but before removing its marker/directory.
        // Untagged orphan files are retained for inspection; never guess that they are disposable.
        val known = recorded().map { it.id }.toSet()
        files.downloadIds().filter { it !in known }.forEach { id ->
            try {
                files.withDownload(id) { directory ->
                    if (directory.removalPending() && recorded().none { it.id == id }) {
                        directory.deletePayloads()
                        directory.finishRemoval()
                    }
                }
            } catch (e: OfflineStorageException) { issues += OfflineRecoveryIssue(id, e.reason) }
        }
        return issues
    }
}
