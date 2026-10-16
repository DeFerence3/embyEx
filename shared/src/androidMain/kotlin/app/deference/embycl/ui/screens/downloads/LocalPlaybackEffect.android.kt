package app.deference.embycl.ui.screens.downloads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.deference.embycl.domain.model.download.*
import java.io.File
import android.net.Uri

actual val localPlaybackSupported = true

internal fun localPlaybackIntent(context: Context, request: LocalPlaybackRequest,
    fileUri: (File) -> Uri = { FileProvider.getUriForFile(context, "${context.packageName}.offline-media", it) },
): Intent {
    require(request.positionMs in 0..Int.MAX_VALUE.toLong()) // mpvEx consumes a signed Int in milliseconds.
    fun uri(path: String) = fileUri(File(path))
    val media = uri(request.media.absolutePath)
    val subtitles = request.subtitles.map { uri(it.file.absolutePath) }
    val selected = subtitles.filterIndexed { index, _ -> request.subtitles[index].selected }.take(1)
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(media, "video/*")
        setClassName("app.marlboroadvance.mpvex", "app.marlboroadvance.mpvex.ui.player.PlayerActivity")
        putExtra("title", request.title)
        putExtra("position", request.positionMs.toInt())
        putParcelableArrayListExtra("playlist", arrayListOf(media))
        putExtra("playlist_index", 0)
        putExtra("subs", subtitles.toTypedArray())
        putExtra("subs.enable", selected.toTypedArray())
        clipData = ClipData.newRawUri("Offline media", media).apply { subtitles.forEach { addItem(ClipData.Item(it)) } }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // No token, HTTP headers, file:// URI, write grant or persistable grant.
    }
}

internal fun localPlaybackResult(resultCode: Int, intent: Intent?): LocalPlaybackResult {
    if (resultCode != Activity.RESULT_OK) return LocalPlaybackResult()
    @Suppress("DEPRECATION")
    fun number(name: String): Long? = runCatching { when (val value = intent?.extras?.get(name)) {
        is Int -> value.toLong().takeIf { it >= 0 }
        is Long -> value.takeIf { it >= 0 }
        else -> null
    } }.getOrNull()
    return LocalPlaybackResult(number("position"), number("duration"))
}

@Composable
actual fun LocalPlaybackEffect(request: LocalPlaybackRequest?, onResult: (String, LocalPlaybackResult) -> Unit) {
    val context = LocalContext.current
    val deliver by rememberUpdatedState(onResult)
    // Preserves registry attribution across Activity recreation; never infer identity from current Session.
    var launchedSession by rememberSaveable { mutableStateOf<String?>(null) }
    var needsFinish by rememberSaveable { mutableStateOf(false) }
    var grantedUris by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    fun releaseGrants() {
        grantedUris.forEach { uri -> runCatching { context.revokeUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        grantedUris = arrayListOf()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            releaseGrants()
            launchedSession?.let { deliver(it, localPlaybackResult(result.resultCode, result.data)) }
            launchedSession = null
        } else if (launchedSession != null) {
            // singleTask players may return CANCELED immediately while still reading the file.
            needsFinish = true
        }
    }
    if (needsFinish) AlertDialog(
        onDismissRequest = {},
        title = { Text("Finish offline playback") },
        text = { Text("mpvEx didn't return a playback position. Close the player, then finish here. Your previous watch progress will be kept.") },
        confirmButton = { TextButton(onClick = {
            releaseGrants()
            launchedSession?.let { deliver(it, LocalPlaybackResult()) }
            launchedSession = null; needsFinish = false
        }) { Text("Finish playback") } },
    )
    LaunchedEffect(request?.sessionId) {
        if (request == null || launchedSession == request.sessionId) return@LaunchedEffect
        if (launchedSession != null) {
            deliver(request.sessionId, LocalPlaybackResult(error = LocalPlayerError.FAILED))
            return@LaunchedEffect
        }
        if (request.positionMs > Int.MAX_VALUE) {
            deliver(request.sessionId, LocalPlaybackResult(error = LocalPlayerError.POSITION_UNSUPPORTED))
            return@LaunchedEffect
        }
        launchedSession = request.sessionId
        try {
            val intent = localPlaybackIntent(context, request)
            grantedUris = ArrayList((0 until intent.clipData!!.itemCount).map { intent.clipData!!.getItemAt(it).uri.toString() })
            launcher.launch(intent)
        }
        catch (_: ActivityNotFoundException) { releaseGrants(); launchedSession = null; deliver(request.sessionId, LocalPlaybackResult(error = LocalPlayerError.NOT_INSTALLED)) }
        catch (_: Exception) { releaseGrants(); launchedSession = null; deliver(request.sessionId, LocalPlaybackResult(error = LocalPlayerError.FAILED)) }
    }
}
