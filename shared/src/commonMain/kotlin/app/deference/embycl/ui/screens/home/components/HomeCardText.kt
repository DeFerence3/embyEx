package app.deference.embycl.ui.screens.home.components

import androidx.compose.runtime.Composable
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_downloaded
import embyex.shared.generated.resources.home_hours_minutes_left
import embyex.shared.generated.resources.home_minutes_left
import embyex.shared.generated.resources.home_movie_year
import embyex.shared.generated.resources.home_next_up
import embyex.shared.generated.resources.home_watched
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeCardUi.kicker(): String? = when {
    seriesName != null && episodeLabel != null -> "$seriesName · $episodeLabel"
    seriesName != null -> seriesName
    type == "Movie" && year != null -> stringResource(
        resource = Res.string.home_movie_year,
        year,
    )
    else -> year?.toString()
}

@Composable
internal fun HomeCardUi.timeLeft(): String? {
    if (kind != HomeCardKind.Resume) return null
    val minutes = minutesLeft ?: return null
    return if (minutes < 60) {
        stringResource(
            resource = Res.string.home_minutes_left,
            minutes,
        )
    } else {
        stringResource(
            resource = Res.string.home_hours_minutes_left,
            minutes / 60,
            minutes % 60,
        )
    }
}

@Composable
internal fun HomeCardUi.accessibilityLabel(): String = listOfNotNull(
    if (kind == HomeCardKind.NextUp) stringResource(resource = Res.string.home_next_up) else null,
    kicker(),
    title,
    timeLeft(),
    if (isWatched) stringResource(resource = Res.string.home_watched) else null,
    if (isDownloaded) stringResource(resource = Res.string.home_downloaded) else null,
).joinToString(separator = ", ")
