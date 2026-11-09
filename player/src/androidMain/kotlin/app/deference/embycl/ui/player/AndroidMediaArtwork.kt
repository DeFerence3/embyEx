package app.deference.embycl.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_ARTWORK_WIDTH = 960
private const val MAX_ARTWORK_BYTES = 16 * 1024 * 1024
private const val TIMEOUT_MILLIS = 15_000

internal fun loadMediaArtwork(
    context: Context,
    uri: String,
    headers: Map<String, String>,
): Bitmap? {
    val parsed = uri.toUri()
    val bytes = when (parsed.scheme?.lowercase()) {
        "http", "https" -> fetch(url = uri, headers = headers)
        else -> context.contentResolver.openInputStream(parsed)?.use { it.readLimited() }
    } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= MAX_ARTWORK_WIDTH) sampleSize *= 2
    return BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private fun fetch(
    url: String,
    headers: Map<String, String>,
): ByteArray? {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        if (connection.responseCode in 200..299) connection.inputStream.use { it.readLimited() } else null
    } finally {
        connection.disconnect()
    }
}

private fun InputStream.readLimited(): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toByteArray()
        if (output.size() + read > MAX_ARTWORK_BYTES) return null
        output.write(buffer, 0, read)
    }
}
