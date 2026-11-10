package app.deference.embycl.ui.player

/** The one dock menu that can be open at a time. */
internal enum class PlayerMenu {
    Subtitles,
    Audio,
    Speed,
    More,
    Aspect,

    /** The Subtitle style page inside the Subtitles menu. */
    SubtitleStyle,

    /** The same page inside More, for videos without a Subtitles button. */
    SubtitleStyleFromMore,
}
