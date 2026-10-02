package app.deference.embycl.ui.core.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AlertDialogDefaults.iconContentColor
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.UpdateStage
import app.deference.embycl.ui.core.markdown.MarkDownPage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

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
        AppUpdate.NotAvailable -> "Yay!!"
        is AppUpdate.Available -> "Update available · ${appUpdate.release.version}"
        is AppUpdate.Downloading -> "Downloading ${appUpdate.release.version}"
        is AppUpdate.ReadyToInstall -> "Ready to install ${appUpdate.release.version}"
        is AppUpdate.AwaitingPermission -> "Allow app updates"
        is AppUpdate.InstallerLaunched -> "Installer opened"
        is AppUpdate.Failed -> when (appUpdate.stage) {
            UpdateStage.Check -> "Could not check for updates"
            UpdateStage.Download -> "Download failed"
            UpdateStage.Install -> "Could not install update"
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
		content = {
			Surface(
				shape = AlertDialogDefaults.shape,
				contentColor = AlertDialogDefaults.textContentColor
			){
				Column(
					modifier = Modifier
						.padding(20.dp),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					CompositionLocalProvider(LocalContentColor provides iconContentColor) {
						Box(Modifier.padding(PaddingValues(bottom = 16.dp)).align(Alignment.CenterHorizontally)) {
							Icon(
								modifier = Modifier,
								imageVector = Icons.Default.SystemUpdateAlt,
								contentDescription = null
							)
						}
					}
					val textStyle = MaterialTheme.typography.headlineSmall
					val contentColor = MaterialTheme.colorScheme.onSurface
					val mergedStyle = LocalTextStyle.current.merge(textStyle)
					CompositionLocalProvider(
						LocalContentColor provides contentColor,
						LocalTextStyle provides mergedStyle,
						content = {
							Box(
								modifier = Modifier
									.align(Alignment.CenterHorizontally)
									.padding(PaddingValues(bottom = 16.dp))
							) {
								Text(
									text = title,
									modifier = Modifier
								)
							}
						},
					)
					
					when (appUpdate) {
						AppUpdate.Idle, AppUpdate.Checking -> {
							LoadingIndicator(
								modifier = Modifier
									.align(Alignment.CenterHorizontally)
							)
							Text("Looking for the latest release…")
						}
						AppUpdate.NotAvailable -> Text("This device already has the latest available version.")
						is AppUpdate.Available -> {
							Text("Download the update, then confirm installation.")
							if (appUpdate.release.changeLogMarkDown.isNotBlank()){
								MarkDownPage(
									markdown = appUpdate.release.changeLogMarkDown,
									modifier = Modifier
										.heightIn(max = 450.dp)
										.align(Alignment.CenterHorizontally)
								)
							}
						}
						is AppUpdate.Downloading -> {
							DebouncedProgressBar(appUpdate)
							Text("You can hide this dialog while the download continues.")
						}
						is AppUpdate.ReadyToInstall -> Text("The download is verified and ready. Please confirm installation if needed.")
						is AppUpdate.AwaitingPermission -> Text("Awaiting permission to install, Your download is saved.")
						is AppUpdate.InstallerLaunched -> Text("Installation in progress. If you cancel, you can install the saved download later.")
						is AppUpdate.Failed -> Text(appUpdate.message, color = MaterialTheme.colorScheme.error)
					}
					
					Row(
						modifier = Modifier
							.fillMaxWidth(),
						horizontalArrangement = Arrangement.End
					) {
						TextButton(onClick = onDismiss) {
							Text(if (appUpdate is AppUpdate.Downloading || appUpdate == AppUpdate.Checking) "Hide" else "Close")
						}
						when (appUpdate) {
							is AppUpdate.Available -> TextButton(onClick = onDownload) { Text("Download update") }
							is AppUpdate.ReadyToInstall -> TextButton(onClick = onInstall) { Text("Install update") }
							is AppUpdate.AwaitingPermission -> TextButton(onClick = onInstall) { Text("Open settings") }
							is AppUpdate.Failed -> TextButton(
								onClick = when (appUpdate.stage) {
									UpdateStage.Check -> onCheck
									UpdateStage.Download -> onDownload
									UpdateStage.Install -> onInstall
								}
							) { Text("Retry") }
							else -> Unit
						}
					}
				}
			}
		}
    )
}

@Composable
private fun DebouncedProgressBar(appUpdate: AppUpdate.Downloading) {
	val targetFraction = appUpdate.fraction ?: 0f
	var debouncedFraction by remember { mutableStateOf(targetFraction) }
	var downloaded by remember { mutableStateOf(appUpdate.downloaded) }
	var total by remember { mutableStateOf(appUpdate.total) }
	
	LaunchedEffect(appUpdate) {
		delay(2.seconds) // few seconds delay ensures we don't choke the Windows Skia thread
		debouncedFraction = targetFraction
		downloaded = appUpdate.downloaded
		total = appUpdate.total
	}
	
	val animatedFraction by animateFloatAsState(targetValue = debouncedFraction)
	
	if (appUpdate.fraction == null) {
		LinearWavyProgressIndicator()
		Text("Downloading…")
	} else {
		LinearWavyProgressIndicator(progress = { animatedFraction })
		Text("${(debouncedFraction * 100).toInt()}% · ${downloaded / 1024 / 1024} / ${total / 1024 / 1024} MB")
	}
}