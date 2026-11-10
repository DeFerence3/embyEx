package app.deference.embycl.ui.player

/** What the feedback pill confirms after a shortcut. Rendered by `PlayerFeedbackPill`. */
internal sealed interface PlayerFeedback {
    data object Played : PlayerFeedback
    data object Paused : PlayerFeedback

    /** [seconds] is negative for a backward seek. */
    data class Seeked(val seconds: Int) : PlayerFeedback

    /** [percent] is 0 to 100. */
    data class Volume(val percent: Int) : PlayerFeedback
    data object Muted : PlayerFeedback
    data class Speed(val speed: Float) : PlayerFeedback
    data class SubtitlesOn(val label: String) : PlayerFeedback
    data object SubtitlesOff : PlayerFeedback
    data class Aspect(val mode: PlayerAspectRatio) : PlayerFeedback

    /** [index] is 1-based. */
    data class Chapter(val index: Int, val name: String) : PlayerFeedback

    /** [percent] is 0 to 90; [positionMillis] is where playback landed. */
    data class Jumped(val percent: Int, val positionMillis: Long) : PlayerFeedback

    /** The key was heard but has nothing to do here, for example no subtitles or no chapters. */
    data object Unavailable : PlayerFeedback
}
