package app.deference.embycl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.deference.embycl.core.utils.buildCrashIssueUri
import kotlinx.coroutines.launch
import java.awt.datatransfer.StringSelection
import java.util.Locale

/**
 * Desktop-only crash UI. Keep this in desktopApp rather than commonMain because
 * diagnostics and the issue-link opener use JVM APIs.
 *
 * [onRestart] should relaunch the process and then terminate this process.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopCrashScreen(
    throwable: Throwable,
    appVersion: String,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
	val environment = remember { getEnvironment() }
	val exceptionString = remember { throwable.stackTraceToString() }
    val report = remember(throwable, appVersion) {
		buildCrashIssueUri(exceptionString,environment).toString()
    }
	val clipboardManager = LocalClipboard.current
	val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
			val compact = maxWidth < 850.dp
			val localUri = LocalUriHandler.current
			val onReport: () -> Unit = {
				val exceptionClip = StringSelection(exceptionString)
				val clipEntry = ClipEntry(exceptionClip)
				scope.launch {
					clipboardManager.setClipEntry(clipEntry)
				}
				actionError = runCatching { localUri.openUri(report) }
					.exceptionOrNull()
					?.let { "Couldn't open browser: ${it.message ?: it::class.simpleName}" }
			}
			val onRestart: () -> Unit = {
				actionError = null
				runCatching(onRestart)
					.onFailure { actionError = "Restart failed: ${it.message ?: it::class.simpleName}" }
			}
            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CrashHero(
                        appVersion = appVersion,
                        platformSummary = platformSummary(),
                        onRestart = onRestart,
                        onReport = onReport,
                        onExit = onExit,
                    )
                    CrashDetailsCard(
                        throwable = throwable,
                        appVersion = appVersion,
                        detailsExpanded = detailsExpanded,
                        onToggleDetails = { detailsExpanded = !detailsExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ActionFeedback(actionError)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        CrashHero(
                            appVersion = appVersion,
                            platformSummary = platformSummary(),
                            onRestart = onRestart,
                            onReport = onReport,
                            onExit = onExit,
                        )
                        ActionFeedback(actionError)
                    }
                    CrashDetailsCard(
                        throwable = throwable,
                        appVersion = appVersion,
                        detailsExpanded = detailsExpanded,
                        onToggleDetails = { detailsExpanded = !detailsExpanded },
                        modifier = Modifier.weight(1.05f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashHero(
    appVersion: String,
    platformSummary: String,
    onRestart: () -> Unit,
    onReport: () -> Unit,
    onExit: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.Start) {
		
		Row {
			Surface(
				modifier = Modifier.size(76.dp),
				color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
				shape = RoundedCornerShape(28.dp),
			) {
				Box(contentAlignment = Alignment.Center) {
					Text(
						text = "!",
						color = MaterialTheme.colorScheme.onErrorContainer,
						style = MaterialTheme.typography.displayMedium,
						fontWeight = FontWeight.Bold,
					)
				}
			}
			Spacer(Modifier.width(12.dp))
			Surface(
				color = MaterialTheme.colorScheme.errorContainer,
				contentColor = MaterialTheme.colorScheme.onErrorContainer,
				shape = RoundedCornerShape(18.dp),
			) {
				Row(
					modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					Text("!", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
					Text(
						text = "EMBYEX  /  RECOVERY MODE",
						style = MaterialTheme.typography.labelLarge,
						fontWeight = FontWeight.SemiBold,
					)
				}
			}
		}
		
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Well, that\ndidn't go\nas planned.",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            lineHeight = 58.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "EmbyEx encountered an unexpected error and couldn't continue. Restart the app, or send a diagnostic report to help track down what happened.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text("Restart EmbyEx")
            }
            OutlinedButton(
                onClick = onReport,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Report issue")
            }
        }
        TextButton(onClick = onExit) {
            Text("Close application")
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetadataPill(label = "VERSION", value = appVersion)
            MetadataPill(label = "PLATFORM", value = platformSummary)
        }
    }
}

@Composable
private fun CrashDetailsCard(
    throwable: Throwable,
    appVersion: String,
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = Icons.Default.BugReport,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.onErrorContainer,
						)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Crash summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Diagnostic details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(18.dp))
            Text(
                text = throwable::class.qualifiedName ?: "Unknown exception",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = throwable.message ?: "No exception message was supplied.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(18.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "RUNTIME ENVIRONMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(9.dp))
                    DiagnosticLine("OS", "${System.getProperty("os.name")} ${System.getProperty("os.version")}")
                    DiagnosticLine("Architecture", System.getProperty("os.arch", "unknown"))
                    DiagnosticLine("Java", System.getProperty("java.runtime.version", System.getProperty("java.version", "unknown")))
                    DiagnosticLine("VM", System.getProperty("java.vm.name", "unknown"))
                    DiagnosticLine("App version", appVersion)
                    DiagnosticLine("Captured", java.time.Instant.now().toString())
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onToggleDetails) {
                Text(if (detailsExpanded) "Hide stack trace" else "Show stack trace")
            }
            AnimatedVisibility(visible = detailsExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = throwable.stackTraceToString(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "GitHub opens with a prefilled report. Review the crash log before submitting; diagnostics may contain file paths or other private data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetadataPill(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ActionFeedback(message: String?) {
    if (message != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun platformSummary(): String = "${System.getProperty("os.name", "Desktop")} · JVM"

private fun getEnvironment(): List<Pair<String, String>>{
	return listOf(
		"App version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_SHA})",
		"Platform" to platformSummary(),
		"OS version" to "${System.getProperty("os.version", "unknown")}",
		"Architecture" to "${System.getProperty("os.arch", "unknown")}",
		"Java runtime" to "${System.getProperty("java.runtime.name", "unknown")} ${System.getProperty("java.runtime.version", System.getProperty("java.version", "unknown"))}",
		"JVM" to "${System.getProperty("java.vm.name", "unknown")} (${System.getProperty("java.vendor", "unknown")})",
		"Language" to "${Locale.getDefault().toLanguageTag()}",
		"Timestamp (UTC)" to "${java.time.Instant.now()}"
	)
}
