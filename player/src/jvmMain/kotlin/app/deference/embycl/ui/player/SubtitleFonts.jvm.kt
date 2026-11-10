package app.deference.embycl.ui.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Writes the bundled subtitle fonts to the user's cache directory and returns it for mpv's `sub-fonts-dir`.
 * Returns null when the directory can't be written; subtitles then fall back to a system font.
 */
internal suspend fun desktopSubtitleFontsDirectory(): File? {
    val fonts = readSubtitleFonts()
    return withContext(context = Dispatchers.IO) {
        try {
            val directory = File(desktopCacheDirectory(), SUBTITLE_FONTS_DIRECTORY)
            if (!directory.isDirectory && !directory.mkdirs()) return@withContext null
            fonts.forEach { (name, bytes) -> writeIfChanged(file = File(directory, name), bytes = bytes) }
            directory
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}

private fun writeIfChanged(
    file: File,
    bytes: ByteArray,
) {
    if (file.isFile && file.length() == bytes.size.toLong()) return
    val partial = File(file.parentFile, "${file.name}.partial")
    partial.writeBytes(array = bytes)
    if (!partial.renameTo(file)) {
        file.delete()
        if (!partial.renameTo(file)) throw IOException("Could not store ${file.name}")
    }
}

private fun desktopCacheDirectory(): File {
    val os = System.getProperty("os.name").orEmpty()
    val home = System.getProperty("user.home")
    fun absoluteEnvironment(name: String) = System.getenv(name)?.takeIf { it.isNotBlank() }
        ?.let(::File)?.takeIf { it.isAbsolute }
    val base = when {
        os.startsWith(prefix = "Windows", ignoreCase = true) -> absoluteEnvironment(name = "LOCALAPPDATA") ?: File(home, "AppData/Local")
        os.startsWith(prefix = "Mac", ignoreCase = true) -> File(home, "Library/Caches")
        else -> absoluteEnvironment(name = "XDG_CACHE_HOME") ?: File(home, ".cache")
    }
    return File(base, "embyEx")
}
