package app.deference.embycl.ui.screens.details.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface PlaybackUi {
    data object Start : PlaybackUi

    data class Resume(
        val position: String,
        val progress: Float,
        val remainingMinutes: Long,
    ) : PlaybackUi

    data object Again : PlaybackUi
}
