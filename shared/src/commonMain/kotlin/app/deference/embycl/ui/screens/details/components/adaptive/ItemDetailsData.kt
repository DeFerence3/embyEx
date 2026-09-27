package app.deference.embycl.ui.screens.details.components.adaptive

import androidx.compose.runtime.Immutable

@Immutable
data class ItemDetailsData(
	val title: String,
	val backdrop: String?,
	val logo: String?,
	val airDate: String?,
	val runtime: String?,
	val resolution: String,
	val audio: String,
	val subtitles: String,
	val overview: String?,
	val directors: List<String>,
	val writers: List<String>,
	val isResume: Boolean,
	val progress: Float,
	val remainingMinutes: Long?,
)