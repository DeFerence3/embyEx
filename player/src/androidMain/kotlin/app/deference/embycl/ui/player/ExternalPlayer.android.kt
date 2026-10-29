package app.deference.embycl.ui.player

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.os.BadParcelableException
import androidx.core.net.toUri

/**
 * Uses mpvEx's explicit activity and alternating name/value header contract.
 * User-Agent must be the first pair: mpvEx skips that pair when reading other HTTP headers.
 * Never log this intent or exceptions: network headers can contain the Emby access token.
 * The caller keeps the embedded player paused and its offline lease alive during the handoff.
 */
internal fun openExternalPlayer(
    source: PlayerSource,
    positionMs: Long,
    launch: (Intent) -> Unit,
): Boolean = try {
    val uri = (source.resolveExternalUri?.invoke() ?: source.uri).toUri()
    val network = uri.scheme == "http" || uri.scheme == "https"
    require(value = network || uri.scheme == "content")
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setClassName(
            "app.marlboroadvance.mpvex",
            "app.marlboroadvance.mpvex.ui.player.PlayerActivity",
        )
        setDataAndType(uri, "video/*")
        putExtra("launch_source", "emby")
        putExtra("title", source.title)
        // mpvEx reads an Int in milliseconds, despite older callers sending a Long.
        putExtra("position", positionMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt())
        putParcelableArrayListExtra("playlist", arrayListOf(uri))
        putExtra("playlist_index", 0)
        if (network) {
            val userAgent = source.headers.entries
                .firstOrNull { it.key.equals(other = "User-Agent", ignoreCase = true) }
                ?.value ?: "mpvEx"
            val headers = buildList {
                add("User-Agent")
                add(userAgent)
                source.headers.forEach { (name, value) ->
                    if (!name.equals(other = "User-Agent", ignoreCase = true)) {
                        add(name)
                        add(value)
                    }
                }
            }
            putExtra("headers", headers.toTypedArray())
        } else {
            clipData = ClipData.newRawUri(source.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    launch(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
} catch (_: IllegalArgumentException) {
    false
}

/** Accept only the integer millisecond formats used by mpvEx versions; ignore malformed results. */
@Suppress("DEPRECATION")
internal fun mpvExPositionMillis(
    data: Intent?,
): Long? = try {
    when (val position = data?.extras?.get("position")) {
        is Int -> position.toLong()
        is Long -> position
        else -> null
    }?.takeIf { it >= 0L }
} catch (_: BadParcelableException) {
    null
}
