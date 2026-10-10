package app.deference.embycl.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.LocalKoasty

@Composable
fun InfoSection(
	title: String,
	isLoading: Boolean,
	onRefresh: () -> Unit,
	details: List<Pair<String, String>>,
	error: String? = null,
	extra: @Composable () -> Unit = {}
) {
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		InfoHeader(title, isLoading = isLoading, onRefresh = onRefresh)
		if (isLoading) {
			Text("Loading information…", style = MaterialTheme.typography.bodyMedium)
		}
		error?.let { error ->
			Text(error, color = MaterialTheme.colorScheme.error)
			TextButton(onClick = onRefresh, enabled = !isLoading) { Text("Retry") }
		}
		extra()
		InfoCard(details)
	}

}

@Composable
fun SettingsSection(
	title: String,
	settings: List<SettingsAction>,
	extra: @Composable () -> Unit = {}
) {
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		InfoHeader(title, isLoading = false, onRefresh = null)
		extra()
		settings.forEachIndexed { index, setting ->
			SettingsCard(
				setting = setting,
				shape = calculateShape(
					index = index,
					lastIndex = settings.lastIndex
				)
			)
		}
	}
}

@Composable
fun InfoHeader(
	title: String,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	onRefresh: (() -> Unit)?
) {
	Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
		onRefresh?.let{
			if (isLoading) {
				CircularProgressIndicator(
					Modifier
						.padding(12.dp)
						.size(24.dp), strokeWidth = 2.dp
				)
			} else {
				IconButton(onClick = it) { Icon(Icons.Outlined.Refresh, "Refresh $title") }
			}
		}
	}
}

@Composable
fun SettingsCard(
	setting: SettingsAction,
	shape: Shape,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier.fillMaxWidth(),
		color = MaterialTheme.colorScheme.surfaceContainerLow,
		shape = shape,
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		){
			Column(
				modifier = Modifier.padding(18.dp),
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				Text(
					text = setting.header,
					style = MaterialTheme.typography.titleMedium
				)
				Text(
					text = setting.description,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			Switch(
				modifier = Modifier.padding(end = 18.dp),
				checked = setting.value,
				onCheckedChange = setting.onAction,
			)
		}
	}
}

@Composable
fun InfoCard(details: List<Pair<String, String>>) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            details.forEachIndexed { index, (label, value) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = calculateShape(index, details.lastIndex),
                ) {
                    Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween
					){
						Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
							Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
							Text(value, style = MaterialTheme.typography.bodyLarge)
						}
						val uriHandler = LocalUriHandler.current
						val koasty = LocalKoasty.current
						val openLink: () -> Unit = {
							try {
								uriHandler.openUri(value)
							} catch (_: Exception) {
								koasty.show("Could not open $value. Check that a browser is available.")
							}
						}
						if (value.startsWith("http")) IconButton(
							modifier = Modifier
								.padding(end = 18.dp),
							onClick = {
								openLink()
							}
						){
							Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open in browser")
						}
					}
                }
            }
        }
    }
}

private fun calculateShape(index: Int,lastIndex: Int) = RoundedCornerShape(
	topStart = if (index == 0) 24.dp else 4.dp,
	topEnd = if (index == 0) 24.dp else 4.dp,
	bottomStart = if (index == lastIndex) 24.dp else 4.dp,
	bottomEnd = if (index == lastIndex) 24.dp else 4.dp,
)