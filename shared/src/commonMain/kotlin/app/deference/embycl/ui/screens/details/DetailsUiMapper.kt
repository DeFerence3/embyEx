package app.deference.embycl.ui.screens.details

import app.deference.embycl.core.utils.asRuntime
import app.deference.embycl.core.utils.formatToString
import app.deference.embycl.domain.model.EmbyItem
import app.deference.embycl.domain.model.EmbyMediaStream
import app.deference.embycl.domain.model.EmbyPerson
import app.deference.embycl.ui.screens.details.model.DetailsUi
import app.deference.embycl.ui.screens.details.model.EpisodeContextUi
import app.deference.embycl.ui.screens.details.model.MediaFactUi
import app.deference.embycl.ui.screens.details.model.PersonKind
import app.deference.embycl.ui.screens.details.model.PersonUi
import app.deference.embycl.ui.screens.details.model.PlaybackUi
import app.deference.embycl.ui.screens.details.model.TrackTag
import app.deference.embycl.ui.screens.details.model.TrackUi
import app.deference.embycl.ui.screens.downloads.formatDownloadBytes
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_fact_audio
import embyex.shared.generated.resources.details_fact_file
import embyex.shared.generated.resources.details_fact_released
import embyex.shared.generated.resources.details_fact_subtitles
import embyex.shared.generated.resources.details_fact_video
import kotlin.math.roundToInt

private const val TICKS_PER_SECOND = 10_000_000L

internal fun EmbyItem.toDetailsUi(): DetailsUi {
    val audio = mediaStreams
        .filter { stream -> stream.type == "Audio" }
        .mapNotNull { stream -> stream.toTrackUi() }
    val subtitles = mediaStreams
        .filter { stream -> stream.type == "Subtitle" }
        .mapNotNull { stream -> stream.toTrackUi() }
        .sortedBy { track -> track.name }
    val videoLabel = mediaStreams
        .firstOrNull { stream -> stream.type == "Video" }
        ?.displayTitle
        ?.takeIf(String::isNotBlank)
        ?: container?.substringBefore(delimiter = ',')?.uppercase()
    return DetailsUi(
        id = id,
        title = name,
        logoUrl = imageUrl(
            type = "Logo",
            maxWidth = 800,
        ),
        backdropUrl = imageUrl(
            type = "Backdrop",
            maxWidth = 1920,
        ) ?: imageUrl(
            type = "Primary",
            maxWidth = 1280,
        ),
        episode = if (isEpisode()) {
            EpisodeContextUi(
                seriesId = seriesId,
                seriesName = seriesName,
                seasonNumber = parentIndexNumber,
                episodeNumber = indexNumber,
            )
        } else {
            null
        },
        year = productionYear?.toString(),
        runtime = runTimeTicks?.asRuntime(),
        officialRating = officialRating?.takeIf(String::isNotBlank),
        communityRating = communityRating?.let { rating -> ((rating * 10).roundToInt() / 10.0).toString() },
        videoLabel = videoLabel,
        genres = genres.filter(String::isNotBlank),
        overview = overview?.takeIf(String::isNotBlank),
        playback = playbackUi(),
        isFavorite = userData?.isFavorite == true,
        isPlayed = userData?.played == true,
        audioTracks = audio,
        subtitleTracks = subtitles,
        people = people.toPeopleUi(withPhotos = useServerArtwork),
        facts = facts(
            videoLabel = videoLabel,
            audio = audio,
            subtitles = subtitles,
        ),
        isAvailableOffline = isAvailableOffline,
    )
}

private fun EmbyItem.playbackUi(): PlaybackUi {
    val position = userData?.playbackPositionTicks ?: 0L
    val duration = runTimeTicks ?: 0L
    return when {
        position > 0L && duration > position -> PlaybackUi.Resume(
            position = position.asClock(),
            progress = (position.toFloat() / duration.toFloat()).coerceIn(
                minimumValue = 0f,
                maximumValue = 1f,
            ),
            remainingMinutes = ((duration - position) / TICKS_PER_SECOND / 60L).coerceAtLeast(minimumValue = 1L),
        )
        userData?.played == true -> PlaybackUi.Again
        else -> PlaybackUi.Start
    }
}

private fun Long.asClock(): String {
    val totalSeconds = this / TICKS_PER_SECOND
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(
        length = 2,
        padChar = '0',
    )
    val ss = seconds.toString().padStart(
        length = 2,
        padChar = '0',
    )
    return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
}

private fun EmbyMediaStream.toTrackUi(): TrackUi? {
    val streamIndex = index ?: return null
    val language = (displayLanguage ?: language)?.takeIf(String::isNotBlank)
    val title = displayTitle?.takeIf(String::isNotBlank)
    val name = language ?: title ?: return null
    return TrackUi(
        index = streamIndex,
        name = name,
        detail = title?.takeIf { it != name },
        tag = if (isForced) TrackTag.Forced else null,
        isDefault = isDefault,
    )
}

private fun List<EmbyPerson>.toPeopleUi(withPhotos: Boolean): List<PersonUi> {
    fun EmbyPerson.toUi(kind: PersonKind): PersonUi? {
        val personName = name?.takeIf(String::isNotBlank) ?: return null
        return PersonUi(
            id = id,
            name = personName,
            role = role?.takeIf(String::isNotBlank),
            kind = kind,
            photoUrl = if (withPhotos) imageUrl() else null,
            initials = personName
                .split(' ')
                .filter(String::isNotBlank)
                .take(n = 2)
                .joinToString(separator = "") { part -> part.first().uppercase() },
        )
    }
    val cast = filter { person -> person.type == "Actor" || person.type == "GuestStar" }
        .mapNotNull { person -> person.toUi(kind = PersonKind.Cast) }
    val directors = filter { person -> person.type == "Director" }
        .mapNotNull { person -> person.toUi(kind = PersonKind.Director) }
    val writers = filter { person -> person.type == "Writer" }
        .mapNotNull { person -> person.toUi(kind = PersonKind.Writer) }
    return (cast + directors + writers).distinctBy { person -> person.id ?: person.name + person.kind }
}

private fun EmbyItem.facts(
    videoLabel: String?,
    audio: List<TrackUi>,
    subtitles: List<TrackUi>,
): List<MediaFactUi> = buildList {
    val released = premiereDate?.formatToString() ?: productionYear?.toString()
    released?.let { value ->
        add(
            MediaFactUi(
                label = Res.string.details_fact_released,
                value = value,
            ),
        )
    }
    videoLabel?.let { value ->
        add(
            MediaFactUi(
                label = Res.string.details_fact_video,
                value = value,
            ),
        )
    }
    if (audio.isNotEmpty()) {
        val languages = audio.map(TrackUi::name).distinct().joinToString(separator = ", ")
        add(
            MediaFactUi(
                label = Res.string.details_fact_audio,
                value = languages,
                trackCount = audio.size,
            ),
        )
    }
    if (subtitles.isNotEmpty()) {
        add(
            MediaFactUi(
                label = Res.string.details_fact_subtitles,
                value = null,
                trackCount = subtitles.size,
            ),
        )
    }
    val source = mediaSources.firstOrNull()
    val file = listOfNotNull(
        source?.container?.substringBefore(delimiter = ',')?.uppercase() ?: container?.uppercase(),
        source?.size?.let { bytes -> formatDownloadBytes(bytes = bytes) },
    ).joinToString(separator = " · ")
    if (file.isNotBlank()) {
        add(
            MediaFactUi(
                label = Res.string.details_fact_file,
                value = file,
            ),
        )
    }
}
