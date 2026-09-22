package app.deference.embycl.platform

import app.deference.embycl.data.preference.EmbyPreference
import java.io.File

/** The preference belongs to the desktop installation and survives account sign-out. */
internal class DesktopPlayerPreferences(private val preferences: EmbyPreference) {
    var path: String
        get() = preferences.getString("desktop_mpv_path") ?: ""
        private set(value) {
            if (value.isBlank()) preferences.remove("desktop_mpv_path")
            else preferences.save("desktop_mpv_path", value)
        }

    fun save(location: String) {
        path = if (location.isBlank()) "" else {
            requireNotNull(resolveMpvLocation(location)) {
                "Choose a folder containing mpv, or select the mpv executable."
            }.absolutePath
        }
    }

    fun executable(): File = requireNotNull(findMpvExecutable(path)) {
        "mpv was not found. Choose its folder or executable in Settings."
    }
}

private val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

internal fun resolveMpvLocation(location: String, windows: Boolean = isWindows): File? {
    if (location.isBlank()) return null
    val file = File(location.trim().removeSurrounding("\""))
    val candidates = if (file.isDirectory) {
        (if (windows) listOf("mpv.exe", "mpv.com") else listOf("mpv")).map(file::resolve)
    } else listOf(file)
    return candidates.firstOrNull {
        it.isFile && if (windows) it.extension.lowercase() in listOf("exe", "com") else it.canExecute()
    }?.absoluteFile
}

internal fun findMpvExecutable(
    configuredPath: String = "",
    environmentOverride: String? = System.getenv("EMBYEX_MPV"),
    searchPath: String? = System.getenv("PATH"),
    windows: Boolean = isWindows,
): File? {
    // An explicitly configured but missing player must be fixed, rather than silently ignored.
    if (configuredPath.isNotBlank()) return resolveMpvLocation(configuredPath, windows)
    if (!environmentOverride.isNullOrBlank()) return resolveMpvLocation(environmentOverride, windows)
    return searchPath.orEmpty().split(File.pathSeparatorChar)
        .filter { it.isNotBlank() }
        .firstNotNullOfOrNull { resolveMpvLocation(it, windows) }
}
