package app.deference.embycl.data.repository.download

/** Bounded per-invocation retry policy; the platform scheduler is a separate component. */
data class DownloadTransferPolicy(
    val maxAttempts: Int = 3,
    val initialBackoffMillis: Long = 1_000,
    val maxInlineWaitMillis: Long = 30_000,
    val headerTimeoutMillis: Long = 30_000,
    val readTimeoutMillis: Long = 30_000,
    val progressIntervalMillis: Long = 500,
) {
    init {
        require(maxAttempts in 1..10 && initialBackoffMillis in 0..30_000 && maxInlineWaitMillis in 0..60_000)
        require(headerTimeoutMillis > 0 && readTimeoutMillis > 0 && progressIntervalMillis >= 0)
    }
    fun backoff(attempt: Int): Long = (initialBackoffMillis * (1L shl attempt.coerceIn(0, 10))).coerceAtMost(maxInlineWaitMillis)
}
