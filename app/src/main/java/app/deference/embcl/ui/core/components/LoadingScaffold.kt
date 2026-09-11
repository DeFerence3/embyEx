package app.deference.embcl.ui.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> LoadingScaffold(
	state: Result<T>?,
	modifier: Modifier = Modifier,
	onRetry: () -> Unit,
	content: @Composable (T) -> Unit,
) {
	Scaffold(
		modifier = modifier
	) { padding ->
		LoadState(state, Modifier.padding(padding), onRetry = onRetry) { item ->
			content(item)
		}
	}
}