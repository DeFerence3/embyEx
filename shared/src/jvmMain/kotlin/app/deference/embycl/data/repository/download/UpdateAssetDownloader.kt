package app.deference.embycl.data.repository.download

import app.deference.embycl.data.repository.update.fileMatchesDigest
import app.deference.embycl.domain.ghrelease.Asset
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

internal class UpdateAssetDownloader(
    private val client: HttpClient,
) {
    suspend fun download(
        asset: Asset,
        onProgress: (Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val digest = asset.digest ?: error("GitHub has no checksum for ${asset.name}. Try again later.")
        // The name comes from the release; it must stay inside the staging directory.
        val isPlainName = asset.name.isNotBlank()
                && asset.name.none { it == '/' || it == '\\' }
                && asset.name != "."
                && asset.name != ".."
        check(isPlainName) { "The release lists an invalid installer name." }
        val directory = Files.createTempDirectory("embyex-update-").toFile()
        val file = File(directory, asset.name)
        try {
            // The proxy avoids unreachable GitHub CDN addresses; the GitHub digest still guards the file.
            client.prepareGet(urlString = "$DOWNLOAD_PROXY${asset.browserDownloadUrl}").execute { response ->
                check(response.status.isSuccess()) { "Could not download the update (HTTP ${response.status.value})." }
                val channel = response.bodyAsChannel()
                file.outputStream().buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var lastReport = 0L
                    while (true) {
                        this@withContext.coroutineContext.ensureActive()
                        val count = channel.readAvailable(buffer)
                        if (count == -1) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        downloaded += count
                        check(downloaded <= asset.size) { "The download is larger than expected." }
                        val now = System.nanoTime()
                        if (now - lastReport >= PROGRESS_INTERVAL_NANOS || downloaded == asset.size) {
                            onProgress(downloaded)
                            lastReport = now
                        }
                    }
                }
            }
            check(file.length() == asset.size) { "The download is incomplete. Try again." }
            check(fileMatchesDigest(file = file, digest = digest)) { "The download is damaged. Try again." }
            file
        } catch (e: Throwable) {
            // Cleanup only; failures and cancellation propagate unchanged.
            directory.deleteRecursively()
            throw e
        }
    }

    private companion object {
        const val DOWNLOAD_PROXY = "https://gh-proxy.com/"
        const val BUFFER_SIZE = 64 * 1024
        const val PROGRESS_INTERVAL_NANOS = 100_000_000L
    }
}
