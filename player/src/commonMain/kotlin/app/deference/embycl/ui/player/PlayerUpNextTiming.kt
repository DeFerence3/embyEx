package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

private const val UP_NEXT_MILLIS = 10_000L

private const val MIN_DURATION_MILLIS = 60_000L

private val CreditsName = Regex(pattern = """\b(credits?|ending|outro)\b""", option = RegexOption.IGNORE_CASE)

internal fun List<PlayerChapter>.creditsStartMillis(
    durationMillis: Long,
): Long? = firstOrNull { it.startMillis >= durationMillis / 2 && CreditsName.containsMatchIn(input = it.name) }?.startMillis

@Composable
internal fun rememberUpNextCountdown(
    player: EmbeddedPlayer?,
    chapters: List<PlayerChapter>,
): State<PlayerUpNextCountdown?> = remember(player, chapters) {
    if (player == null) MutableStateFlow(value = null) else combine(player.currentPositionMillis, player.mediaProperties) { position, properties ->
        upNextCountdown(
            positionMillis = position,
            durationMillis = properties?.durationMillis ?: 0L,
            chapters = chapters,
        )
    }.distinctUntilChanged()
}.collectAsState(initial = null)

private fun upNextCountdown(
    positionMillis: Long,
    durationMillis: Long,
    chapters: List<PlayerChapter>,
): PlayerUpNextCountdown? {
    if (durationMillis <= MIN_DURATION_MILLIS) return null
    val credits = chapters.creditsStartMillis(durationMillis = durationMillis)
        ?.takeIf { it < durationMillis - UP_NEXT_MILLIS }
    if (credits != null) {
        if (positionMillis < credits) return null
        return PlayerUpNextCountdown(
            secondsLeft = ((credits + UP_NEXT_MILLIS - positionMillis) / 1_000L).toInt().coerceIn(minimumValue = 0, maximumValue = 10),
            endsMedia = false,
        )
    }
    val secondsLeft = ((durationMillis - positionMillis) / 1_000L).toInt()
    return if (secondsLeft in 0..10) PlayerUpNextCountdown(secondsLeft = secondsLeft, endsMedia = true) else null
}

@Composable
internal fun UpNextAutoplayEffect(
    countdown: PlayerUpNextCountdown?,
    upNext: PlayerQueueEntry?,
    autoplay: Boolean,
    onNavigate: (PlayerQueueEntry) -> Unit,
) {
    val navigate by rememberUpdatedState(newValue = onNavigate)
    val due = upNext?.takeIf { autoplay && countdown != null && !countdown.endsMedia && countdown.secondsLeft == 0 }
    LaunchedEffect(key1 = due) { if (due != null) navigate(due) }
}
