package app.deference.embcl.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
fun InfoHeader(
	title: String,
	modifier: Modifier = Modifier,
	isLoading: Boolean = false,
	onRefresh: () -> Unit
) {
	Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
		if (isLoading) {
			CircularProgressIndicator(
				Modifier
					.padding(12.dp)
					.size(24.dp), strokeWidth = 2.dp
			)
		} else {
			IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, title) }
		}
	}
}

@Composable
fun InfoCard(details: List<Pair<String, String>>) {
	OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
		SelectionContainer {
			Column(Modifier.padding(horizontal = 16.dp)) {
				details.forEachIndexed { index, (label, value) ->
					if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
					Column(
						Modifier
							.fillMaxWidth()
							.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
					) {
						Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
						Text(value, style = MaterialTheme.typography.bodyLarge)
					}
				}
			}
		}
	}
}