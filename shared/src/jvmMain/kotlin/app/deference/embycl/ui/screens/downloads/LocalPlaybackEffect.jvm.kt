package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.*
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.download.*
import app.deference.embycl.platform.DesktopPlayerPreferences
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit

actual val localPlaybackSupported = true

@Composable
actual fun LocalPlaybackEffect(request: LocalPlaybackRequest?, onResult: (String, LocalPlaybackResult) -> Unit) {
    val deliver by rememberUpdatedState(onResult)
    LaunchedEffect(request?.sessionId) {
        if (request == null) return@LaunchedEffect
        val executable = try { withContext(Dispatchers.IO) { DesktopPlayerPreferences(Session.preferences).executable().absolutePath } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { deliver(request.sessionId, LocalPlaybackResult(error = LocalPlayerError.NOT_INSTALLED)); return@LaunchedEffect }
        val result = try { launchLocalMpv(request, executable) }
        catch (e: CancellationException) { throw e }
        catch (_: IOException) { LocalPlaybackResult(error = LocalPlayerError.FAILED) }
        catch (_: IllegalArgumentException) { LocalPlaybackResult(error = LocalPlayerError.FAILED) }
        deliver(request.sessionId, result)
    }
}

internal fun localMpvArguments(request: LocalPlaybackRequest, executable: String, script: String): List<String> {
    require(request.sessionId.matches(Regex("[a-zA-Z0-9-]+")))
    require(request.positionMs in 0..Long.MAX_VALUE / 10_000)
    val paths = listOf(request.media.absolutePath) + request.subtitles.map { it.file.absolutePath }
    require(paths.all { File(it).isAbsolute && it.none { char -> char == '\n' || char == '\r' || char == '\u0000' } })
    return buildList {
        add(executable)
        // A predictable single-file session: user auto-play scripts/watch-later must not change attribution.
        addAll(listOf("--no-config", "--load-scripts=no", "--resume-playback=no", "--save-position-on-quit=no",
            "--sub-auto=no", "--audio-file-auto=no", "--fs", "--force-media-title=${request.title}",
            "--start=${request.positionMs / 1000.0}", "--script=$script",
            "--script-opt=embyex-local-session=${request.sessionId}", "--script-opt=embyex-local-path=${request.media.absolutePath}",
            "--terminal=yes", "--msg-level=all=warn,embyex_local=info"))
        request.subtitles.forEach { add("--sub-file=${it.file.absolutePath}") }
        add("--"); add(request.media.absolutePath)
    }
}

internal fun localMpvResult(line: String, sessionId: String): LocalPlaybackResult? {
    val match = Regex("^\\[embyex_local] EMBYEX_LOCAL_${Regex.escape(sessionId)} ([0-9.]+) ([0-9.]+|unknown) (eof|stopped)$").matchEntire(line.trim()) ?: return null
    fun millis(value: String): Long? = value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && it <= (Long.MAX_VALUE / 10_000) / 1000.0 }?.let { (it * 1000).toLong() }
    val position = millis(match.groupValues[1]) ?: return null
    return LocalPlaybackResult(position, millis(match.groupValues[2]), match.groupValues[3] == "eof")
}

internal suspend fun launchLocalMpv(
    request: LocalPlaybackRequest, executable: String,
    start: (List<String>) -> Process = { ProcessBuilder(it).redirectErrorStream(true).start() },
): LocalPlaybackResult = withContext(Dispatchers.IO) {
    val directory = Files.createTempDirectory("embyex-local-playback-").toFile()
    val script = directory.resolve("embyex_local.lua")
    try {
        script.writeText(LOCAL_MPV_PROGRESS_SCRIPT)
        val process = start(localMpvArguments(request, executable, script.absolutePath))
        coroutineScope {
            val stop = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() }
                finally {
                    process.destroy()
                    if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
                    runCatching { process.inputStream.close() }
                }
            }
            try {
                runInterruptible(Dispatchers.IO) {
                    var result = LocalPlaybackResult()
                    // Bounded lines: discard all other output instead of retaining media/player logs.
                    process.inputStream.bufferedReader().use { reader ->
                        val line = StringBuilder(); var overflow = false
                        while (true) {
                            val char = reader.read()
                            if (char == -1) break
                            if (char == '\n'.code) {
                                if (!overflow) localMpvResult(line.toString(), request.sessionId)?.let { result = it }
                                line.setLength(0); overflow = false
                            } else if (line.length < 4096) line.append(char.toChar()) else overflow = true
                        }
                        if (!overflow) localMpvResult(line.toString(), request.sessionId)?.let { result = it }
                    }
                    if (process.waitFor() == 0) result else LocalPlaybackResult(error = LocalPlayerError.FAILED)
                }
            } finally { withContext(NonCancellable) { stop.cancelAndJoin() } }
        }
    } finally {
        // Delete only the two known artifacts we created; never recurse through an external path.
        script.delete(); directory.delete()
    }
}

internal val LOCAL_MPV_PROGRESS_SCRIPT = """
    local mp = require 'mp'
    local options = { session = '', path = '' }
    require('mp.options').read_options(options, 'embyex-local')
    local selected = false
    local position = nil
    local duration = nil
    mp.register_event('file-loaded', function()
        selected = mp.get_property('path') == options.path and mp.get_property_number('playlist-pos', -1) == 0
        position = nil
        duration = mp.get_property_number('duration')
    end)
    mp.observe_property('time-pos', 'number', function(_, value)
        if selected and value ~= nil then position = value end
    end)
    mp.observe_property('duration', 'number', function(_, value)
        if selected and value ~= nil then duration = value end
    end)
    mp.register_event('end-file', function(event)
        if selected and event.reason == 'eof' and duration ~= nil then position = duration end
        if selected and position ~= nil then
            local reason = event.reason == 'eof' and 'eof' or 'stopped'
            mp.msg.info('EMBYEX_LOCAL_' .. options.session .. ' ' .. string.format('%.3f', position) .. ' ' ..
                (duration ~= nil and string.format('%.3f', duration) or 'unknown') .. ' ' .. reason)
        end
        selected = false
    end)
""".trimIndent()
