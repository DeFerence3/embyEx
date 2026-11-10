package app.deference.embycl.ui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import org.openani.mediamp.MediaStatus
import org.openani.mediamp.PlaybackEvent
import org.openani.mediamp.features.AspectRatioMode
import org.openani.mediamp.features.AudioLevelController
import org.openani.mediamp.features.MediaMetadata
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.features.VideoAspectRatio
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.source.MediaExtraFiles
import org.openani.mediamp.source.Subtitle
import org.openani.mediamp.source.UriMediaData

/** Keeps the desktop engine and all MediaMP types inside jvmMain. */
internal class JvmEmbeddedPlayer(
    val engine: MpvPlayer,
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
    override val playbackSpeed: PlayerSpeed = engine.features[PlaybackSpeed]?.let { speed ->
        object : PlayerSpeed {
            override val valueFlow = speed.valueFlow
            override val value get() = speed.value

            override fun set(
                speed: Float,
            ) {
                this@JvmEmbeddedPlayer.engine.features[PlaybackSpeed]?.set(speed = speed)
            }
        }
    } ?: object : PlayerSpeed {
        // mpv keeps the speed for the lifetime of this player instance, so the last value set is current.
        private val current = MutableStateFlow(value = 1f)
        override val valueFlow = current.asStateFlow()
        override val value get() = current.value

        override fun set(
            speed: Float,
        ) {
            if (this@JvmEmbeddedPlayer.engine.setSpeed(speed = speed)) current.value = speed
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

    override val audioLevel = engine.features[AudioLevelController]?.let { level ->
        object : PlayerAudioLevel {
            // mpv's level is a fraction where 1 is the source level; the player caps it there.
            override val volume = level.volume
            override val muted = level.isMute

            override fun setVolume(
                volume: Float,
            ) = level.setVolume(volume = volume.coerceIn(minimumValue = 0f, maximumValue = 1f))

            override fun setMuted(
                muted: Boolean,
            ) = level.setMute(mute = muted)
        }
    }
    override val chapters = engine.features[MediaMetadata]?.chapters?.let { chapters ->
        chapters
            .map { list -> list.map { PlayerChapter(name = it.name, startMillis = it.offsetMillis) }.sortedBy { it.startMillis } }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )
    }

    override fun setSubtitleStyle(
        style: PlayerSubtitleStyle,
    ) {
        // A rejected property keeps mpv's default for it; the rest still apply.
        style.mpvProperties().forEach { (name, value) -> engine.setProperty(name = name, value = value) }
    }

    override suspend fun open(
        source: PlayerSource,
        playWhenReady: Boolean,
        startPositionMillis: Long,
    ) {
        // libass reads the fonts directory when a subtitle track loads, so set it before the media.
        desktopSubtitleFontsDirectory()?.let { directory ->
            engine.setProperty(name = "sub-fonts-dir", value = directory.absolutePath)
        }
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
