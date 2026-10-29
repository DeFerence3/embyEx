package app.deference.embycl.ui.player

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode

internal class AndroidMpvMetadata(
    private val isReady: () -> Boolean,
) {
    val audioTracks = AndroidMpvTrackGroup(
        trackId = { it.id },
        selectNative = { select(property = "aid", id = it) },
    )
    val subtitleTracks = AndroidMpvTrackGroup(
        trackId = { it.id },
        selectNative = { select(property = "sid", id = it) },
    )
    private var externalSubtitles = emptyMap<String, PlayerSubtitle>()

    fun setExternalSubtitles(
        subtitles: List<PlayerSubtitle>,
    ) {
        externalSubtitles = subtitles.associateBy { it.uri }
    }

    fun refresh() {
        if (!isReady()) return
        val tracks = MPVLib.getPropertyNode(property = "track-list")?.asArray().orEmpty()
        audioTracks.refresh(
            tracks = tracks.filter { it["type"]?.asString() == "audio" }.mapNotNull { track ->
                val id = track["id"]?.asInt()?.toString() ?: return@mapNotNull null
                PlayerTrack(
                    id = id,
                    name = track["title"]?.asString() ?: track["lang"]?.asString(),
                    labels = labels(track = track),
                )
            },
            selectedId = MPVLib.getPropertyString(property = "aid"),
        )
        subtitleTracks.refresh(
            tracks = tracks.filter { it["type"]?.asString() == "sub" }.mapNotNull { track ->
                val id = track["id"]?.asInt()?.toString() ?: return@mapNotNull null
                PlayerTrack(
                    id = id,
                    language = externalSubtitle(track = track)?.language ?: track["lang"]?.asString(),
                    labels = labels(track = track),
                )
            },
            selectedId = MPVLib.getPropertyString(property = "sid"),
        )
    }

    fun clear() {
        audioTracks.clear()
        subtitleTracks.clear()
        externalSubtitles = emptyMap()
    }

    private fun select(
        property: String,
        id: String,
    ): Boolean {
        if (!isReady()) return false
        MPVLib.setPropertyString(property = property, value = id)
        return MPVLib.getPropertyString(property = property) == id
    }

    private fun externalSubtitle(
        track: MPVNode,
    ): PlayerSubtitle? = track["external-filename"]?.asString()?.let { externalSubtitles[it] }

    private fun labels(
        track: MPVNode,
    ): List<PlayerTrackLabel> = listOfNotNull(
        externalSubtitle(track = track)?.label ?: track["title"]?.asString(),
        externalSubtitle(track = track)?.language ?: track["lang"]?.asString(),
        track["codec"]?.asString(),
    ).distinct().map { label ->
        PlayerTrackLabel(
            language = null,
            value = label,
        )
    }
}
