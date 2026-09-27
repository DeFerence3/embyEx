package app.deference.embycl.ui.screens.details.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.deference.embycl.core.session.Session
import app.deference.embycl.platform.DesktopPlayerPreferences
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.screens.details.EmbyDetailsAction
import app.deference.embycl.ui.screens.details.EmbyDetailsEvent
import app.deference.embycl.ui.screens.details.EmbyPlaybackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files

@Composable
actual fun PlaybackEffect(events: Flow<EmbyDetailsEvent>, onAction: (EmbyDetailsAction) -> Unit) {
	var error by remember { mutableStateOf<String?>(null) }
	events.ObserveEvent { event ->
		when (event) {
			is EmbyDetailsEvent.Error -> Unit
			is EmbyDetailsEvent.LaunchPlayback -> {
				try {
					val positionMs = launchMpv(event.request)
					onAction(EmbyDetailsAction.PlaybackFinished(positionMs))
				} catch (_: IOException) {
					error = "Could not start mpv. Choose its folder or executable in Settings, or install it on PATH."
				} catch (e: IllegalArgumentException) {
					error = e.message
				} catch (e: IllegalStateException) {
					error = e.message
				}
			}
		}
	}
	error?.let { message ->
		AlertDialog(
			onDismissRequest = { error = null },
			title = { Text("Playback unavailable") },
			text = { Text(message) },
			confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
		)
	}
}

internal fun mpvArguments(request: EmbyPlaybackRequest, executable: String, playlist: String, script: String): List<String> {
	require(request.selectedIndex in request.urls.indices)
	return listOf(
		executable,
		"--fs",
		"--playlist=$playlist",
		"--playlist-start=${request.selectedIndex}",
		"--force-media-title=${request.title}",
		"--user-agent=EmbyEx",
		"--http-header-fields=X-Emby-Token: ${request.accessToken}",
		"--script=$script",
		"--script-opts=embyex-index=${request.selectedIndex},embyex-start=${request.positionMs / 1000.0}",
		"--terminal=yes",
		"--msg-level=all=warn,embyex=info",
	)
}

private suspend fun launchMpv(request: EmbyPlaybackRequest): Long? = withContext(Dispatchers.IO) {
	val executable = DesktopPlayerPreferences(Session.preferences).executable().absolutePath
	val directory = Files.createTempDirectory("embyex-playback-").toFile()
	val playlist = directory.resolve("playlist.m3u")
	val script = directory.resolve("embyex.lua")
	try {
		require(request.urls.all { !it.contains('\n') && !it.contains('\r') })
		playlist.writeText("#EXTM3U\n" + request.urls.joinToString("\n"))
		script.writeText(MPV_PROGRESS_SCRIPT)
		val process = ProcessBuilder(mpvArguments(request, executable, playlist.absolutePath, script.absolutePath))
			.redirectErrorStream(true).start()
		var positionMs: Long? = null
		// Drain the pipe without retaining mpv's output, which can contain authenticated URLs.
		process.inputStream.bufferedReader().useLines { lines ->
			lines.forEach { line ->
				Regex("EMBYEX_POSITION=([0-9.]+)").find(line)?.groupValues?.get(1)
					?.toDoubleOrNull()?.let { positionMs = (it * 1000).toLong() }
			}
		}
		check(process.waitFor() == 0) { "mpv could not play this media. Check the server connection and mpv installation." }
		positionMs
	} finally {
		playlist.delete()
		script.delete()
		directory.delete()
	}
}

// Resume only the selected episode, and never attribute a later episode's position to it.
private val MPV_PROGRESS_SCRIPT = """
    local mp = require 'mp'
    local options = { index = 0, start = 0 }
    require('mp.options').read_options(options, 'embyex')
    local selected = false
    local position = nil
    local duration = nil
    local resumed = false
    mp.register_event('file-loaded', function()
        selected = mp.get_property_number('playlist-pos', -1) == options.index
        position = nil
        duration = mp.get_property_number('duration')
        if selected and not resumed then
            resumed = true
            if options.start > 0 then mp.commandv('seek', options.start, 'absolute', 'exact') end
        end
    end)
    mp.observe_property('time-pos', 'number', function(_, value)
        if selected and value ~= nil then position = value end
    end)
    mp.register_event('end-file', function(event)
        if event.reason == 'eof' and duration ~= nil then position = duration end
        if selected and position ~= nil then mp.msg.info('EMBYEX_POSITION=' .. position) end
        selected = false
    end)
""".trimIndent()
