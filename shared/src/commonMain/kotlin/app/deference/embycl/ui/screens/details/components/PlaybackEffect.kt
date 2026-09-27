package app.deference.embycl.ui.screens.details.components

import androidx.compose.runtime.Composable
import app.deference.embycl.ui.screens.details.EmbyDetailsAction
import app.deference.embycl.ui.screens.details.EmbyDetailsEvent
import kotlinx.coroutines.flow.Flow

@Composable
expect fun PlaybackEffect(events: Flow<EmbyDetailsEvent>, onAction: (EmbyDetailsAction) -> Unit)