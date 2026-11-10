"""Desktop player, Android player, and the menus / panel page."""
from . import menus as m
from .lib import (LONG_KICKER, LONG_NAME, MOVIE_KICKER, MOVIE_NAME, feedback, fullscreen, go, landscape, page, pip, portrait, screen, seekfx, snack, state,
                  to, windowed)

HOST = go("shell/host/saving")
MENUS = dict(subs=go("menus/subtitles/default"), audio=go("menus/audio/default"), speed=go("menus/speed/default"), more=go("menus/more/default"),
             mute=go("menus/volume/muted"))


def win_r(play="paused", panel="closed", **extra):
    r = dict(play=to(play), panel=to(panel), fs=go("desktop/fullscreen/visible"), pip=go("desktop/pip/hover"), back=HOST,
             tab_ch=to("playing"), tab_up=to("upnext"), **MENUS)
    r.update(extra)
    return r


def full_r(play="paused", panel="panel", **extra):
    r = dict(play=to(play), panel=to(panel), fs=go("desktop/windowed/playing"), pip=go("desktop/pip/hover"), back=HOST,
             tab_ch=to("panel"), tab_up=to("panel"), **MENUS)
    r.update(extra)
    return r


# ---------------------------------------------------------------- desktop
def desktop_page():
    w = [
        state("playing", "Playing", "Playback running, window at least 1100 px wide", windowed(routes=win_r()),
              "Nothing covers the picture, so the controls never hide in windowed mode. Try Play, the panel button, Fullscreen and PiP."),
        state("paused", "Paused", "Space, K, media key, or Play", windowed(paused=True, routes=win_r("playing")),
              "Play turns round and the timeline goes flat: two cues besides the icon."),
        state("closed", "Panel closed", "P, or the panel button", windowed(panel="closed", routes=win_r(panel="playing")),
              "The picture grows and centres. The current chapter moves to the end of the heading."),
        state("upnext", "Up next tab", "Up next tab selected", windowed(panel="upnext", routes=win_r()),
              "The queue for this season, and the switch that turns automatic play of the next episode on or off."),
        state("seeking", "Seeking by keyboard", "Focus on the timeline, or ← → J L from anywhere",
              windowed(kb=True, fb=feedback("forward_5", "+5 s"), routes=win_r()),
              "The bubble shows the target time and chapter for pointer and keyboard alike. The feedback pill confirms each key press."),
        state("opening", "Opening", "Player opened, first frame not decoded yet", windowed(opening=True, routes=win_r("playing")),
              "The artwork is shown dimmed; one spinner, in place of Play. Timeline is disabled until the length is known. Back works."),
        state("buffering", "Buffering", "Playback stalled waiting for data", windowed(buffering=True, routes=win_r("playing")),
              "Only Play changes: the user's intent (playing) is kept, so nothing else jumps."),
        state("upnext-count", "Episode ending · Up next", "Last 10 seconds of an episode, autoplay on",
              windowed(upnext="count", routes=win_r(next=to("opening"), cancel=to("playing"))),
              "Enter plays now, Esc cancels. The card is the one thing allowed over the picture in windowed mode, and only for these seconds."),
        state("upnext-manual", "Up next · no countdown", "Reduced motion, a screen reader, or autoplay switched off",
              windowed(upnext="manual", flat=True, routes=win_r(next=to("opening"), cancel=to("playing"))),
              "Nothing starts by itself. With reduced motion the timeline is also flat."),
        state("movie", "Movie", "A film: no episodes around it",
              windowed(movie=True, kicker=MOVIE_KICKER, name=MOVIE_NAME, side_variant="chapters-only", routes=win_r()),
              "Previous and Next leave the dock; the panel shows chapters only, without tabs."),
        state("nochapters", "Movie without chapters", "Nothing to list: no chapters and no queue",
              windowed(movie=True, kicker=MOVIE_KICKER, name=MOVIE_NAME, panel="none", chapters=False, routes=win_r()),
              "No panel and no panel button. The picture takes the room."),
        state("long", "Long title", "Series and episode names at 3× typical length", windowed(kicker=LONG_KICKER, name=LONG_NAME, routes=win_r()),
              "Both lines truncate to one line; the full text is in the tooltip and is what a screen reader hears."),
        state("failed", "Playback failed", "The engine reported an error",
              windowed(failed="failed", routes=win_r(retry=to("opening"))),
              "T5 inside the picture area: the content can't be shown, but heading and Back stay so nobody is trapped. One message, because the engine can't tell a network failure from an unsupported format."),
        state("offline", "Offline", "Starting a stream with no connection",
              windowed(failed="offline", routes=win_r(retry=to("opening"), downloads=HOST)),
              "T5 with the way out as the primary action."),
        state("narrow", "Narrow window (760 px)", "Window under 940 px wide", windowed(narrow=True, panel="closed", routes=win_r()),
              "Under 1100 px the panel is closed by default and opens over the picture. Under 940 px the volume slider folds into its button, and Speed and PiP move into More."),
    ]
    f = [
        state("visible", "Controls visible", "Pointer moved, any key, or tap", fullscreen(routes=full_r()),
              "The only gradient is the short one behind the heading; the dock is opaque. Controls hide after 3 s (longer with an accessibility timeout)."),
        state("hidden", "Controls hidden", "3 s without input while playing", fullscreen(hidden=True),
              "Subtitles drop back to their normal position. Shortcuts keep working without bringing the controls back."),
        state("feedback", "Shortcut while hidden", "↑ pressed with controls hidden", fullscreen(hidden=True, fb=feedback("volume_up", "65 %", meter=65)),
              "Only the feedback pill appears, for about 1.3 s. It is a status region, so the change is also announced."),
        state("paused", "Paused", "Space, K, media key, or click on the picture", fullscreen(paused=True, routes=full_r("visible")),
              "Controls stay while paused."),
        state("panel", "Panel open", "P, or the panel button", fullscreen(panel="chapters", routes=full_r(panel="visible")),
              "The picture resizes into the free area so subtitles are never covered."),
        state("buffering", "Buffering", "Playback stalled", fullscreen(buffering=True, routes=full_r())),
        state("upnext", "Episode ending · Up next", "Last 10 seconds, autoplay on",
              fullscreen(upnext="count", routes=full_r(next=to("visible"), cancel=to("visible"))),
              "Appears even when the controls are hidden, above the dock position."),
        state("failed", "Playback failed", "The engine reported an error", fullscreen(failed="failed", routes=full_r(retry=to("visible"))),
              "T5. Esc still steps back to windowed, then closes."),
    ]
    pr = dict(restore=go("desktop/windowed/playing"), back=HOST)
    p = [
        state("hover", "Pointer over or focused", "Pointer enters the window, or any key", pip("hover", routes=dict(play=to("paused"), **pr)),
              "A small dock, Restore and Close. Space, arrows, M and the other shortcuts work in this window too. Esc restores."),
        state("resting", "Resting", "No input for 3 s", pip("resting"),
              "Just the picture, subtitles and a progress edge. The window is moved by its title bar or by dragging the picture."),
        state("paused", "Paused", "Space or Play", pip("paused", routes=dict(play=to("hover"), **pr)), "Controls stay while paused."),
        state("min", "Minimum size (320 × 180)", "Window resized to its smallest",
              f'<div class="px-desk"><div class="px-window pipmin">{pip("min", routes=pr)}</div></div>',
              "Only Play and Restore remain. Seeking stays on the keyboard."),
        state("buffering", "Buffering", "Playback stalled", pip("buffering", routes=pr)),
        state("upnext", "Episode ending", "Last 10 seconds, autoplay on", pip("upnext", routes=dict(cancel=to("hover"), **pr)),
              "One line and Cancel; there is no room for the card."),
        state("failed", "Playback failed", "The engine reported an error", pip("failed", routes=dict(retry=to("hover"), **pr)),
              "Short form of the T5 message. Restore shows the full text."),
    ]
    body = "\n".join([
        screen("windowed", "Windowed", "The player inside the app window. Primary action: play or pause while seeing the whole picture.", w),
        screen("fullscreen", "Fullscreen", "Edge to edge. Primary action: play or pause; everything else gets out of the way.", f),
        screen("pip", "PiP mini window", "Always-on-top window, 480 × 270 by default. Primary action: play or pause; second: restore.", p, "f-fixed f-pip"),
    ])
    return page("desktop", "Desktop player", "Player",
                "One dock in every mode. The timeline is a full-width line with only the two timers beside it; every other control sits in the row below.", body)


# ---------------------------------------------------------------- android
def android_page():
    pr = dict(play=to("paused"), lock=to("locked"), rotate=go("android/landscape/visible"), pip=to("pip-refused"), subs=go("menus/sheets/subtitles"),
              audio=go("menus/sheets/audio"), speed=go("menus/sheets/speed"), more=go("menus/sheets/more"), back=HOST, tab_ch=to("playing"), tab_up=to("upnext"))
    po = [
        state("playing", "Playing", "Phone held upright", portrait(routes=pr),
              "The controls sit under the picture and never hide in portrait. The chapter list keeps the playing chapter in view."),
        state("paused", "Paused", "Play, or a tap on the picture", portrait(paused=True, routes=dict(pr, play=to("playing")))),
        state("upnext", "Up next tab", "Up next tab selected", portrait(panel="upnext", routes=pr)),
        state("seek", "Double-tap seek", "Double tap on the right half of the picture", portrait(gesture=seekfx("fwd", 10), routes=pr),
              "Repeated taps add up (+20 s, +30 s). The same amount is announced."),
        state("level", "Volume drag", "Vertical drag on the right half; brightness on the left", portrait(fb=feedback("volume_up", "65 %", meter=65), routes=pr),
              "Same feedback pill as the keyboard, so touch and keys read alike."),
        state("locked", "Locked", "Lock tile", portrait(locked=True),
              "Everything but the unlock slider is gone, so a pocket or a child can't change anything. Back shows the slider again."),
        state("buffering", "Buffering", "Playback stalled", portrait(buffering=True, routes=pr)),
        state("upnext-count", "Episode ending · Up next", "Last 10 seconds, autoplay on",
              portrait(upnext="count", panel="none", routes=dict(pr, next=to("playing"), cancel=to("playing"))),
              "The card takes the panel's place under the picture for these seconds."),
        state("movie", "Movie without chapters", "Nothing to list", portrait(movie=True, panel="none", chapters=False, kicker=MOVIE_KICKER, name=MOVIE_NAME, routes=pr),
              "The picture centres in the free space."),
        state("long", "Long title", "Names at 3× typical length", portrait(kicker=LONG_KICKER, name=LONG_NAME, routes=pr),
              "The title clamps to two lines in portrait, the series line to one."),
        state("pip-refused", "PiP refused", "Android declined picture in picture",
              portrait(routes=pr, snack=snack("Couldn't start picture in picture. Allow it for EmbyEx in system settings.", "Settings")),
              "T3: an event with one action. Today this is a text line that never leaves."),
        state("external", "mpvEx missing", "More → Open in mpvEx, app not installed",
              portrait(routes=pr, snack=snack("Couldn't open mpvEx. Check that it's installed.")), "T3. Replaces today's toast."),
        state("failed", "Playback failed", "The engine reported an error", portrait(failed="failed", routes=dict(pr, retry=to("playing"))),
              "T5. Android adds the hand-off to mpvEx as a second way forward."),
    ]
    lr = dict(play=to("paused"), lock=to("locked"), panel=to("panel"), subs=to("subs"), back=HOST)
    la = [
        state("visible", "Controls visible", "Tap, or any key on a connected keyboard", landscape(routes=lr),
              "Timeline above the control row (today it is below). Lock, Rotate and Speed left; Subtitles, Audio, panel and More right."),
        state("hidden", "Controls hidden", "3 s without input while playing", landscape(hidden=True)),
        state("panel", "Panel open", "Panel button", landscape(panel="chapters", routes=dict(lr, panel=to("visible"))),
              "The picture resizes so subtitles stay visible."),
        state("locked", "Locked", "Lock button", landscape(locked=True), "Slide to unlock; the slider itself hides after 2 s and returns on tap."),
        state("seek", "Double-tap seek", "Double tap on the left half", landscape(hidden=True, gesture=seekfx("back", 10))),
        state("level", "Brightness drag", "Vertical drag on the left half", landscape(hidden=True, fb=feedback("brightness_medium", "40 %", meter=40))),
        state("paused", "Paused", "Play, or a tap", landscape(paused=True, routes=dict(lr, play=to("visible")))),
        state("buffering", "Buffering", "Playback stalled", landscape(buffering=True, routes=lr)),
        state("upnext", "Episode ending · Up next", "Last 10 seconds, autoplay on", landscape(upnext="count", routes=dict(lr, next=to("visible"), cancel=to("visible")))),
        state("subs", "Subtitles (side sheet)", "Subtitles button", landscape(routes=lr, overlay=m.side_sheet("Subtitles", m.subtitles())),
              "In landscape the sheets open at the right edge, where the panel also opens."),
        state("failed", "Playback failed", "The engine reported an error", landscape(failed="failed", routes=dict(lr, retry=to("visible"))), "T5."),
    ]
    tl = [
        state("playing", "Playing with panel", "Tablet held sideways", landscape(panel="chapters", routes=dict(panel=to("closed"))),
              "The phone landscape layout with room to keep the panel open. Hardware keyboard shortcuts work as on desktop."),
        state("closed", "Panel closed", "Panel button or P", landscape(routes=dict(panel=to("playing")))),
        state("hidden", "Controls hidden", "3 s without input", landscape(hidden=True)),
    ]
    tp = [state("playing", "Playing", "Tablet held upright", portrait(tabp=True), "The phone portrait order with a three-pill dock instead of tiles.")]
    body = "\n".join([
        screen("portrait", "Phone portrait", "Picture on top, chapters in the middle, controls within thumb reach. Primary action: play or pause.", po, "f-fixed f-port"),
        screen("landscape", "Phone landscape", "The picture fills the screen. Primary action: play or pause.", la, "f-fixed f-land"),
        screen("tablet", "Tablet landscape", "Same layout as phone landscape, panel open by default.", tl, "f-fixed f-tab"),
        screen("tabletp", "Tablet portrait", "Same order as phone portrait.", tp, "f-fixed f-tabp"),
    ])
    return page("android", "Android player", "Player",
                "Same dock, same timeline. Gestures stay as they are today: tap for controls, double tap to seek, drag for volume and brightness.", body)


# ---------------------------------------------------------------- menus and panel
def menus_page():
    back = to("default")
    r = win_r()
    style_go = go("menus/style/default")
    sub = [
        state("default", "Track selected", "Subtitles button or S", windowed(routes=r, menu=("subs", m.menu("Subtitles", m.subtitles(style_attrs=style_go), key="S"))),
              "Radio rows; the header shows the shortcut that opens this menu. C switches subtitles on and off without opening it."),
        state("off", "Off", "Off chosen, or C", windowed(routes=r, sub=False, menu=("subs", m.menu("Subtitles", m.subtitles("off", style_go), key="S")))),
        state("many", "24 tracks", "A file with many subtitle tracks", windowed(routes=r, menu=("subs", m.menu("Subtitles", m.subtitles("many", style_go), key="S", scroll=True))),
              "Grouped by language, Off stays first, the list scrolls inside the menu. Typing a letter jumps to that language."),
        state("none", "No subtitles", "The file has no subtitle tracks", windowed(routes=r, sub=False, menu=("subs", m.menu("Subtitles", m.subtitles("none", style_go), key="S"))),
              "The button stays enabled so the reason can be read."),
        state("failed", "Track failed to load", "An external subtitle file didn't download",
              windowed(routes=r, snack=snack("Couldn't load Deutsch subtitles.", "Retry")),
              "T3. The selection goes back to the track that was showing."),
    ]

    def styled(face="nunito", size="Medium", background="Box", **kw):
        cls = " ".join(filter(None, [{"nunito": "", "atkinson": "atkinson", "barlow": "barlow"}[face], {"Box": "", "Outline": "outline", "Shadow": "shadow"}[background],
                                     "lg" if size in ("Large", "Larger") else ""]))
        body = m.style(face, size, background, reset_attrs=to("reset"))
        return windowed(routes=r, sub_cls=cls, menu=("style", m.menu("Subtitle style", body, back=go("menus/subtitles/default"))), **kw)

    st = [
        state("default", "Default", "Subtitles → Subtitle style", styled(),
              "Changes apply at once to the picture behind and to the preview; there is no Save. The app UI stays on the system font."),
        state("large", "Large with outline", "Size Large, Background Outline", styled(size="Large", background="Outline")),
        state("atkinson", "Atkinson Hyperlegible", "Typeface changed", styled("atkinson"), "Each typeface name is set in its own face."),
        state("barlow", "Barlow Semi Condensed", "Typeface changed", styled("barlow")),
        state("reset", "Reset", "Reset to default", windowed(routes=r, snack=snack("Subtitle style reset.", "Undo")), "T3 with Undo; no confirmation."),
    ]
    au = [
        state("default", "Tracks", "Audio button or A", windowed(routes=r, menu=("audio", m.menu("Audio track", m.audio(), key="A"))),
              "Language first, then layout and codec on the second line."),
        state("single", "Single track", "The file has one audio track", windowed(routes=r, single_audio=True),
              "The button is disabled and says why on hover and focus."),
    ]
    sp = [state("default", "Speed", "Speed button, or < and >", windowed(routes=r, menu=("speed", m.menu("Playback speed", m.speed(), end=True))))]
    mo = [
        state("default", "More", "More button", windowed(routes=r, menu=("more", m.menu("More", m.more(routes=dict(aspect=to("aspect"), style=style_go,
              keys=go("keyboard/cheatsheet/default"))), end=True))), "Things used rarely. Keyboard shortcuts is always the last row."),
        state("aspect", "Aspect ratio", "More → Aspect ratio, or Z to cycle", windowed(routes=r, menu=("more", m.menu("Aspect ratio", m.aspect(), key="Z", back=back, end=True)))),
        state("narrow", "More in a narrow window", "Window under 940 px",
              windowed(narrow=True, panel="closed", routes=r, menu=("more", m.menu("More", m.more(narrow=True), end=True))),
              "Speed and Picture in picture move here when the dock is compact."),
    ]
    vo = [
        state("default", "Volume", "Pointer over Mute", windowed(routes=r, tip="mute"), "↑ and ↓ change volume by 5 %; the slider takes arrow keys when focused."),
        state("muted", "Muted", "M, or the Mute button", windowed(routes=dict(r, mute=to("default")), muted=True, fb=feedback("volume_off", "Muted")),
              "The icon changes and the slider greys out but keeps its position, so Unmute returns to the same level."),
        state("popover", "Narrow window", "Window under 940 px, Mute button hovered or focused", windowed(narrow=True, panel="closed", routes=r, volpop=True),
              "The slider folds into a popover above the button."),
    ]
    pa = [
        state("chapters", "Chapters", "Panel open", windowed(routes=r), "One Tab stop for the list; ↑ ↓ move, Enter jumps. The playing row is marked by fill, weight and icon."),
        state("upnext", "Up next", "Up next tab", windowed(panel="upnext", routes=r)),
        state("loading", "Loading", "Chapters not received yet", windowed(side_variant="loading", routes=r), "Skeleton rows; the rest of the player is unaffected."),
        state("failed", "Up next failed", "The queue request failed", windowed(panel="upnext", side_variant="failed", routes=dict(r, retry=to("upnext"))),
              "Partial failure: the message sits inside the panel with its own retry."),
        state("auto-off", "Autoplay off", "Switch turned off", windowed(panel="upnext", side_variant="auto-off", routes=r), "The Up next card then waits for a click."),
        state("next-season", "Last episode of the season", "S2 · E8 playing", windowed(panel="upnext", side_variant="next-season", routes=r),
              "The queue continues into the next season and says so."),
        state("finale", "Series finale", "No episode follows", windowed(panel="upnext", side_variant="finale", routes=r),
              "No autoplay switch, because there is nothing to play."),
        state("many", "18 chapters, long names", "A film with many chapters", windowed(side_variant="many", routes=r),
              "Names truncate with the full text in the tooltip; times past one hour widen the column."),
    ]
    pr = dict(subs=to("subtitles"), audio=to("audio"), speed=to("speed"), more=to("more"))
    sh = [
        state("subtitles", "Subtitles", "Subtitles tile", portrait(routes=pr, overlay=m.sheet("Subtitles", m.subtitles(style_attrs=to("style")))),
              "Same rows as the desktop menu, 52 px tall."),
        state("style", "Subtitle style", "Subtitles → Subtitle style", portrait(routes=pr, overlay=m.sheet("Subtitle style", m.style(), back=to("subtitles")))),
        state("audio", "Audio", "Audio tile", portrait(routes=pr, overlay=m.sheet("Audio track", m.audio()))),
        state("speed", "Speed", "Speed tile", portrait(routes=pr, overlay=m.sheet("Playback speed", m.speed()))),
        state("more", "More", "More tile", portrait(routes=pr, overlay=m.sheet("More", m.more(android=True, routes=dict(style=to("style"))))),
              "Android adds Open in mpvEx."),
    ]
    body = "\n".join([
        screen("subtitles", "Subtitles", "Pick a subtitle track, or none.", sub),
        screen("style", "Subtitle style", "Make subtitles readable: typeface, size, background.", st),
        screen("audio", "Audio", "Pick an audio track.", au),
        screen("speed", "Speed", "Change playback speed.", sp),
        screen("more", "More", "Rarely used options.", mo),
        screen("volume", "Volume", "Set the level or mute.", vo),
        screen("panel", "Chapters and Up next", "Jump inside this video, or see what plays after it.", pa),
        screen("sheets", "Sheets on phone", "The same menus as bottom sheets.", sh, "f-fixed f-port"),
    ])
    return page("menus", "Menus and panel", "Player",
                "Menus open upwards from the dock pill that owns them; on phone the same rows come as a bottom sheet. Esc or Back closes them.", body)
