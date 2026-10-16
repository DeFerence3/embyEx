package app.deference.embycl.domain.model.download

/** Outcome of asking the repository to download an item. */
sealed interface DownloadRequest {
	/** Caller shows a source picker; [selected] is preset when only one source exists. */
	data class Choose(
		val title: String,
		val sources: List<DownloadSource>,
		val selected: DownloadKey? = null
	) : DownloadRequest
	data class Failed(val message: String) : DownloadRequest
}