package app.deference.embycl.ui.core.update

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.ui.core.components.ExpressiveEmblem
import app.deference.embycl.ui.core.markdown.MarkDownPage
import kotlin.math.round

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateAvailableDialog(
    appUpdate: AppUpdate,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (appUpdate) {
        AppUpdate.Idle, AppUpdate.Checking -> "Checking for updates"
        AppUpdate.NotAvailable -> "You’re up to date"
        is AppUpdate.Available -> "Update available · ${appUpdate.release.tag}"
        is AppUpdate.Downloading -> "Downloading ${appUpdate.release.tag}"
        is AppUpdate.ReadyToInstall -> "Ready to install ${appUpdate.release.tag}"
        is AppUpdate.AwaitingPermission -> "Allow app updates"
        is AppUpdate.InstallerLaunched -> "Installer opened"
        is AppUpdate.Failed -> when (appUpdate.stage) {
            UpdateStage.Check -> "Could not check for updates"
            UpdateStage.Download -> "Download failed"
            UpdateStage.Install -> "Could not install update"
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints {
            val compactWindow = maxHeight < 480.dp
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = 560.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (compactWindow) Icon(Icons.Default.SystemUpdateAlt, null, tint = MaterialTheme.colorScheme.primary)
                        else ExpressiveEmblem(Icons.Default.SystemUpdateAlt)
                        Text(title, style = MaterialTheme.typography.headlineSmall)
                        when (appUpdate) {
                            AppUpdate.Idle, AppUpdate.Checking -> {
                                LoadingIndicator(Modifier.align(Alignment.CenterHorizontally))
                                Text("Looking for the latest release…")
                            }
                            AppUpdate.NotAvailable -> Text("This device has the latest available version of EmbyEx.")
                            is AppUpdate.Available -> {
                                Text("Download the update, then confirm installation.")
                                if (appUpdate.release.changeLogMarkDown.isNotBlank()) MarkDownPage(
                                    appUpdate.release.changeLogMarkDown, Modifier.fillMaxWidth().heightIn(max = 280.dp))
                            }
                            is AppUpdate.Downloading -> {
                                DebouncedProgressBar(appUpdate)
                                Text("You can hide this dialog while the download continues.")
                            }
                            is AppUpdate.ReadyToInstall -> Text("The download is verified and ready to install.")
                            is AppUpdate.AwaitingPermission -> Text("Allow installation to continue. Your download is saved.")
                            is AppUpdate.InstallerLaunched -> Text("Complete installation in the installer. You can install the saved download later if you cancel.")
                            is AppUpdate.Failed -> Text(appUpdate.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text(if (appUpdate is AppUpdate.Downloading || appUpdate == AppUpdate.Checking) "Hide" else "Close") }
                        when (appUpdate) {
                            is AppUpdate.Available -> Button(onClick = onDownload) { Text("Download update") }
                            is AppUpdate.ReadyToInstall -> Button(onClick = onInstall) { Text("Install update") }
                            is AppUpdate.AwaitingPermission -> Button(onClick = onInstall) { Text("Open settings") }
                            is AppUpdate.Failed -> Button(onClick = when (appUpdate.stage) {
                                UpdateStage.Check -> onCheck
                                UpdateStage.Download -> onDownload
                                UpdateStage.Install -> onInstall
                            }) { Text("Retry") }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebouncedProgressBar(appUpdate: AppUpdate.Downloading) {
	val fraction = appUpdate.fraction
	val downloadedBytes = appUpdate.downloaded
	if (fraction == null) {
		LinearWavyProgressIndicator(
			modifier = Modifier
				.fillMaxWidth()
		)
		Text("Downloading… ${downloadedBytes.bytesToMB.formatToDecimal()} MB")
	} else {
		val animatedFraction by animateFloatAsState(targetValue = fraction)
		LinearWavyProgressIndicator(
			progress = { animatedFraction },
			modifier = Modifier
				.fillMaxWidth()
		)
		Row(
			modifier = Modifier
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text("${downloadedBytes.bytesToMB.formatToDecimal()} / ${appUpdate.total.bytesToMB.formatToDecimal()} MB")
			Text("${(fraction * 100).formatToDecimal()}%")
		}
	}
}

private fun Float.formatToDecimal(): String{
	val decimals = 2
	var multiplier = 1L
	repeat(decimals) { multiplier *= 10 }
	val rounded = round(this * multiplier).toLong()
	val whole = rounded / multiplier
	val fraction = (rounded % multiplier).toString().padStart(decimals, '0')
	return "$whole.$fraction"
}

private fun Double.formatToDecimal(): String{
	val decimals = 2
	var multiplier = 1L
	repeat(decimals) { multiplier *= 10 }
	val rounded = round(this * multiplier).toLong()
	val whole = rounded / multiplier
	val fraction = (rounded % multiplier).toString().padStart(decimals, '0')
	return "$whole.$fraction"
}

val Long.bytesToMB: Double
	get() = this / (1024.0 * 1024.0)
