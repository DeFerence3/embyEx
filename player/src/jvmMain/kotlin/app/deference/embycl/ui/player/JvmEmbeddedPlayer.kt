package app.deference.embycl.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.PlaybackEvent
import org.openani.mediamp.features.AspectRatioMode
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.features.VideoAspectRatio
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData

/** Keeps the desktop engine and all MediaMP types inside jvmMain. */
internal class JvmEmbeddedPlayer(
    val engine: TaoMpvPlayer,
    scope: CoroutineScope,
) : EmbeddedPlayer {
    override val state = engine.state.map { it.toPlayerState() }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = engine.state.value.toPlayerState(),
    )
    override val currentPositionMillis = engine.currentPositionMillis
    override val mediaProperties = engine.mediaProperties.map { properties ->
        properties?.let {
            PlayerMediaProperties(
                durationMillis = it.durationMillis,
                videoWidth = it.videoWidth,
                videoHeight = it.videoHeight,
            )
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
    override val events = engine.events.mapNotNull { event ->
        when (event) {
            is PlaybackEvent.MediaEnded -> PlayerEvent.MediaEnded(
                finalPositionMillis = event.finalPositionMillis,
                durationMillis = event.durationMillis,
            )
            is PlaybackEvent.SeekCompleted -> PlayerEvent.SeekCompleted(positionMillis = event.positionMillis)
            is PlaybackEvent.ErrorOccurred -> PlayerEvent.ErrorOccurred
            is PlaybackEvent.ExternalPlayWhenReadyChanged -> null
        }
    }
    override val audioTracks = engine.audioTracks?.let { tracks ->
        JvmPlayerTrackGroup(
            delegate = tracks,
            scope = scope,
            id = { it.id },
            toPlayerTrack = { track ->
                PlayerTrack(
                    id = track.id,
                    name = track.name,
                    labels = track.labels.map { PlayerTrackLabel(value = it.value, language = it.language) },
                )
            },
        )
    }
    override val subtitleTracks = engine.subtitleTracks?.let { tracks ->
        JvmPlayerTrackGroup(
            delegate = tracks,
            scope = scope,
            id = { it.id },
            toPlayerTrack = { track ->
                PlayerTrack(
                    id = track.id,
                    language = track.language,
                    labels = track.labels.map { PlayerTrackLabel(value = it.value, language = it.language) },
                )
            },
        )
    }
    override val playbackSpeed = engine.features[PlaybackSpeed]?.let { speed ->
        object : PlayerSpeed {
            override val valueFlow = speed.valueFlow
            override val value get() = speed.value

            override fun set(
                speed: Float,
            ) {
                this@JvmEmbeddedPlayer.engine.features[PlaybackSpeed]?.set(speed = speed)
            }
        }
    }
    override val aspectRatio = engine.features[VideoAspectRatio]?.let { aspect ->
        object : PlayerAspect {
            override val mode = aspect.mode.map { it.toPlayerAspectRatio() }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = aspect.mode.value.toPlayerAspectRatio(),
            )

            override fun setMode(
                mode: PlayerAspectRatio,
            ) {
                aspect.setMode(
                    mode = when (mode) {
                        PlayerAspectRatio.FIT -> AspectRatioMode.FIT
                        PlayerAspectRatio.CROP -> AspectRatioMode.CROP
                        PlayerAspectRatio.STRETCH -> AspectRatioMode.STRETCH
                    },
                )
            }
        }
    }

    override suspend fun open(
        source: PlayerSource,
        playWhenReady: Boolean,
        startPositionMillis: Long,
    ) {
        engine.setMediaData(
            data = UriMediaData(
                uri = source.uri,
                headers = source.headers,
                extraFiles = MediaExtraFiles(
                    subtitles = source.subtitles.map {
                        Subtitle(
                            uri = it.uri,
                            mimeType = it.mimeType,
                            language = it.language,
                            label = it.label,
                        )
                    },
                ),
            ),
            playWhenReady = playWhenReady,
            startPositionMillis = startPositionMillis,
        )
    }

    override fun play() = engine.play()
    override fun pause() = engine.pause()
    override fun seekTo(
        positionMillis: Long,
    ) = engine.seekTo(positionMillis = positionMillis)

    override fun close() = engine.close()
}

private fun org.openani.mediamp.PlayerState.toPlayerState() = PlayerState(
    mediaStatus = when (mediaStatus) {
        MediaStatus.Idle -> PlayerMediaStatus.Idle
        MediaStatus.Opening -> PlayerMediaStatus.Opening
        MediaStatus.Ready -> PlayerMediaStatus.Ready
        MediaStatus.Ended -> PlayerMediaStatus.Ended
        is MediaStatus.Error -> PlayerMediaStatus.Error
        MediaStatus.Released -> PlayerMediaStatus.Released
    },
    playWhenReady = playWhenReady,
    isBuffering = isBuffering,
)

private fun AspectRatioMode.toPlayerAspectRatio(): PlayerAspectRatio = when (this) {
    AspectRatioMode.FIT -> PlayerAspectRatio.FIT
    AspectRatioMode.CROP -> PlayerAspectRatio.CROP
    AspectRatioMode.STRETCH -> PlayerAspectRatio.STRETCH
}
