package app.deference.embycl.domain.model.download

/** [message] is user-facing; null means nothing to report. */
data class DownloadResult(val ok: Boolean, val message: String? = null)