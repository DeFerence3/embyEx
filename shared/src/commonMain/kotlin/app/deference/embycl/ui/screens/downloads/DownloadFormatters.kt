package app.deference.embycl.ui.screens.downloads

import kotlin.math.roundToInt

internal fun formatDownloadBytes(
    bytes: Long,
): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KiB", "MiB", "GiB", "TiB", "PiB", "EiB")
    var size = bytes.toDouble() / 1024
    var index = 0
    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index++
    }
    val tenths = (size * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10} ${units[index]}"
}

