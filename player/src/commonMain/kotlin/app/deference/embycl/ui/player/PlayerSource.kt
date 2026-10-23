package app.deference.embycl.ui.player

import org.openani.mediamp.source.Subtitle

/** An in-memory source. Never log authenticated media URLs or headers. */
class PlayerSource(
    val key: String,
    val title: String,
    val uri: String,
    val positionMs: Long,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<Subtitle> = emptyList(),
)
