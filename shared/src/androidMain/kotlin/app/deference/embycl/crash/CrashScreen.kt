package app.deference.embycl.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.shared.R
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.crash_screen_copy_logs
import embyex.shared.generated.resources.crash_screen_logcat_loading
import embyex.shared.generated.resources.crash_screen_logcat_unavailable
import embyex.shared.generated.resources.crash_screen_report
import embyex.shared.generated.resources.crash_screen_report_error
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
fun CrashScreen(
	exceptionString: String,
	logcat: String?,
	modifier: Modifier = Modifier,
	onReport: () -> Unit,
	onCopyLogs: () -> Unit,
	onRestartApp: () -> Unit,
	reportFailed: Boolean = false,
) {
	Scaffold(
		modifier = modifier.fillMaxSize(),
		bottomBar = {
			val borderColor = MaterialTheme.colorScheme.outline
			Column(
				Modifier
					.windowInsetsPadding(NavigationBarDefaults.windowInsets)
					.drawBehind {
						drawLine(
							borderColor,
							Offset.Zero,
							Offset(size.width, 0f),
							strokeWidth = Dp.Hairline.value,
						)
					}
					.padding(vertical = 8.dp, horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(4.dp),
			) {
				if (reportFailed) {
					Text(
						text = stringResource(resource = Res.string.crash_screen_report_error),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.error,
					)
				}
				Row(
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					Button(
						onClick = onReport,
						modifier = Modifier.weight(1f),
					) {
						Text(text = stringResource(resource = Res.string.crash_screen_report))
					}
					FilledIconButton(
						onClick = onCopyLogs,
						enabled = logcat != null,
					) {
						Icon(
							imageVector = Icons.Default.ContentCopy,
							contentDescription = stringResource(resource = Res.string.crash_screen_copy_logs),
						)
					}
				}
				OutlinedButton(
					onClick = onRestartApp,
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(androidStringResource(R.string.crash_screen_restart))
				}
			}
		},
	) { paddingValues ->
		Column(
			modifier = Modifier
				.padding(paddingValues)
				.padding(horizontal = 16.dp)
				.verticalScroll(rememberScrollState()),
			verticalArrangement = Arrangement.spacedBy(16.dp),
		) {
			Spacer(Modifier.height(paddingValues.calculateTopPadding()))
			Icon(
				Icons.Outlined.BugReport,
				null,
				modifier = Modifier.size(48.dp),
				tint = MaterialTheme.colorScheme.primary,
			)
			Text(
				androidStringResource(R.string.crash_screen_title),
				style = MaterialTheme.typography.headlineLarge,
			)
			Text(
				androidStringResource(R.string.crash_screen_subtitle, androidStringResource(R.string.app_name)),
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Text(
				androidStringResource(R.string.crash_screen_logs_title),
				style = MaterialTheme.typography.headlineSmall,
			)
			LogsContainer(exceptionString)
			Text(
				"Logcat:",
				style = MaterialTheme.typography.headlineSmall,
			)
			LogsContainer(
				logs = when {
					logcat == null -> stringResource(resource = Res.string.crash_screen_logcat_loading)
					logcat.isBlank() -> stringResource(resource = Res.string.crash_screen_logcat_unavailable)
					else -> logcat
				},
			)
			Spacer(Modifier.height(8.dp))
		}
	}
}

@Composable
fun LogsContainer(
	logs: String,
	modifier: Modifier = Modifier,
) {
	LazyRow(
		modifier = modifier
			.clip(RoundedCornerShape(16.dp))
			.background(MaterialTheme.colorScheme.surfaceVariant),
	) {
		item {
			SelectionContainer {
				Text(
					text = logs,
					fontFamily = FontFamily.Monospace,
					style = MaterialTheme.typography.labelMedium,
					modifier = Modifier.padding(8.dp),
				)
			}
		}
	}
}
