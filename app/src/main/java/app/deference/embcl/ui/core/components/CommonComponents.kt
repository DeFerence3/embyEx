package app.deference.embcl.ui.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
	TopAppBar(
		title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
		navigationIcon = {
			IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
		},
	)
}

@Composable
fun <T> LoadState(
	state: Result<T>?,
	modifier: Modifier = Modifier,
	onRetry: () -> Unit,
	content: @Composable (T) -> Unit,
) {
	Box(modifier.fillMaxSize()) {
		when {
			state == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
			state.isFailure -> ErrorState(state.exceptionOrNull()?.message ?: "Something went wrong.", onRetry)
			else -> content(state.getOrThrow())
		}
	}
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Text("Couldn’t load Emby", style = MaterialTheme.typography.titleLarge)
		Spacer(Modifier.height(8.dp))
		SelectionContainer{
			Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		Spacer(Modifier.height(16.dp))
		FilledTonalButton(onClick = onRetry) {
			Icon(Icons.Filled.Refresh, null)
			Spacer(Modifier.width(8.dp))
			Text("Try again")
		}
	}
}

@Composable
fun EmptyState(title: String, message: String) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(32.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
		Spacer(Modifier.height(14.dp))
		Text(title, style = MaterialTheme.typography.titleLarge)
		Spacer(Modifier.height(6.dp))
		Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

@Composable
fun <T> Flow<T>.ObserveEvent(onEvent: suspend (T) -> Unit) {
	val flow = this
	val lifecycleOwner = LocalLifecycleOwner.current
	LaunchedEffect(flow, lifecycleOwner.lifecycle) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			withContext(Dispatchers.Main.immediate) {
				flow.collect(onEvent)
			}
		}
	}
}
