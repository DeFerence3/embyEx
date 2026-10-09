package app.deference.embycl.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.platform.DesktopPlayerPreferences
import app.deference.embycl.platform.findMpvExecutable
import java.io.File
import javax.swing.JFileChooser

internal actual val hasPlayerSettings = true

@Composable
internal actual fun PlayerSettings() {
	val preferences = remember { DesktopPlayerPreferences(Session.preferences) }
	var activePath by remember { mutableStateOf(findMpvExecutable(preferences.path)?.absolutePath) }
	var path by remember { mutableStateOf(activePath ?: preferences.path) }
	var error by remember { mutableStateOf<String?>(null) }

	fun save(value: String) {
		try {
			preferences.save(value)
			path = preferences.path
			activePath = findMpvExecutable(path)?.absolutePath
			error = null
		} catch (e: IllegalArgumentException) {
			error = e.message
		}
	}

	Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Text("Desktop player", style = MaterialTheme.typography.titleLarge)
			Text("Choose the mpv folder or executable. Leave blank to use EMBYEX_MPV or PATH.")
			OutlinedTextField(
				value = path,
                shape = MaterialTheme.shapes.medium,
				onValueChange = { path = it; error = null },
				label = { Text("mpv folder or executable") },
				singleLine = true,
				isError = error != null,
				modifier = Modifier.fillMaxWidth(),
			)
			error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
				OutlinedButton(onClick = {
					val chooser = JFileChooser().apply {
						dialogTitle = "Choose mpv folder or executable"
						fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
						val initial = path.ifBlank { activePath.orEmpty() }
						if (initial.isNotBlank()) selectedFile = File(initial)
					}
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						path = chooser.selectedFile.absolutePath
						error = null
					}
				}) { Text("Browse…") }
				Button(onClick = { save(path) }) { Text("Save") }
				TextButton(
					onClick = {
						val mpvInstalled = findMpvExecutable(preferences.path)?.absolutePath
						if (mpvInstalled != null) {
							activePath = mpvInstalled
							error = null
							save(mpvInstalled)
						}else{
							error = "mpv was not found in env variables or path, please choose manually."
						}
					}
				) { Text("Use automatic") }
			}
			if(activePath == null) Text(
				"mpv was not found. Select its folder or executable above.",
				style = MaterialTheme.typography.bodySmall,
			)
		}
	}
}
