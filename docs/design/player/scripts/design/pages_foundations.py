"""Design system page and the shell & behaviour page."""
from . import menus as m
from .lib import (SUB_LINE, app, art, btn, feedback, fullscreen, go, ic, keys, kbd, next_card, page, pill, portrait, screen, side, snack, state, tile,
                  timeline, title, to, vol, windowed)


def fig(html, caption, cls=""):
    return f'<figure class="{cls}">{html}<figcaption>{caption}</figcaption></figure>'


def section(key, heading, text, specimen, note="", dark=False):
    panel = ' data-theme-panel="dark"' if dark else ""
    n = f'<p class="v-note">{note}</p>' if note else ""
    return f'<div class="v-section" id="{key}"><h2>{heading}</h2><p>{text}</p><div class="v-specimen app"{panel}>{specimen}</div>{n}</div>'


def system_page():
    icon_names = ("arrow_back,play_arrow,pause,replay_10,forward_10,replay_5,forward_5,skip_previous,skip_next,volume_up,volume_off,music_note,subtitles,subtitles_off,"
                  "speed,more_vert,more_horiz,right_panel_open,right_panel_close,picture_in_picture_alt,fullscreen,fullscreen_exit,open_in_full,close,segment,"
                  "playlist_play,tune,aspect_ratio,lock,screen_rotation,brightness_medium,fast_forward,fast_rewind,keyboard,restart_alt,search,error,cloud_off,flag,"
                  "open_in_new,chevron_right,info,warning,block,check_circle")
    plain = btn("subtitles", "Subtitles")
    states_ = "".join(fig(btn("subtitles", "Subtitles", c), cap) for c, cap in
                      [("", "default"), ("is-hover", "hover"), ("is-focus", "focus"), ("is-pressed", "pressed · squircle"), ("is-disabled", "disabled")])
    plays = "".join(fig(btn(i, lab, c), cap) for i, lab, c, cap in
                    [("play_arrow", "Play", "play is-paused", "paused · round"), ("pause", "Pause", "play", "playing · squircle"),
                     ("pause", "Pause", "play is-hover", "hover"), ("pause", "Pause", "play is-focus", "focus"), ("pause", "Pause", "play is-pressed", "pressed"),
                     ("pause", "Loading", "play is-buffering", "buffering")])
    pills = (fig(pill(btn("volume_up", "Mute") + vol() + '<span class="pl-sep"></span>' + btn("music_note", "Audio track") + btn("subtitles", "Subtitles", attrs='aria-expanded="true"')),
                 "side pill · Subtitles menu open")
             + fig(pill(btn("skip_previous", "Previous episode") + btn("replay_10", "Back 10 s") + btn("pause", "Pause", "play")
                        + btn("forward_10", "Forward 10 s") + btn("skip_next", "Next episode"), "dock"), "transport pill")
             + fig('<div class="pl-pill tag">24:16 / 52:04</div>', "tag pill (PiP time)"))
    tl = "".join(fig(f'<div class="sp-pl" style="width:560px;padding-top:{40 if b else 0}px">{pill(timeline(**k), "line")}</div>', cap) for k, cap, b in [
        ({}, "playing · wavy", False), ({"flat": True}, "paused or reduced motion · flat", False),
        ({"focus": True, "bubble": (55.0, "28:40", "The crossing")}, "keyboard focus · seek bubble", True), ({"disabled": True}, "opening · disabled", False)])
    tips = (fig(f'<div style="padding-top:44px">{btn("volume_up", "Mute", "is-hover", key="M", tip=True)}</div>', "tooltip names the key")
            + fig(keys(["Space", "K"]) + " " + keys(["Shift+N"]) + " " + kbd("Esc"), "key caps"))
    fbk = (fig(feedback("forward_10", "+10 s"), "seek") + fig(feedback("volume_up", "65 %", meter=65), "level") + fig(feedback("volume_off", "Muted", muted=True), "muted")
           + fig(feedback("block", "No previous episode", muted=True), "not available here"))
    menu_sp = fig(f'<div style="position:relative">{m.menu("Subtitles", m.subtitles(), key="S").replace(" up", "")}</div>', "menu · radio rows, shortcut in header")
    panel_sp = fig(f'<div style="height:520px;display:flex">{side()}</div>', "panel · Chapters tab") + fig(f'<div style="height:520px;display:flex">{side("upnext")}</div>', "panel · Up next tab")
    rows = "".join(fig(f'<ul class="pl-list" style="width:300px"><li><button class="pl-chap-row {c}"{a}><span class="num">{n}</span><span>The crossing</span><span class="at">19:05</span></button></li></ul>', cap)
                   for c, a, n, cap in [("", "", "4", "default"), ("is-hover", "", "4", "hover"), ("is-focus", "", "4", "focus"), ("", ' aria-current="true"', ic("play_arrow"), "playing")])
    cards = fig(next_card(), "pinned in the panel") + fig(next_card("count"), "countdown · last 10 s") + fig(next_card("manual"), "no countdown")
    tl_ = fig(f'<div style="width:360px">' + '<div class="pl-tiles">' + tile("subtitles", "Subtitles", "English") + tile("music_note", "Audio", "English 5.1")
              + tile("speed", "Speed", "1×") + tile("lock", "Lock") + "</div></div>", "phone tiles · label + current value")
    subs = "".join(f'<p class="{c}" style="font-family:var(--font-{f})"><span>{SUB_LINE}</span><small>{lab}</small></p>' for f, c, lab in [
        ("subtitle", "", "Nunito · default · box"), ("subtitle", "outline", "Nunito · outline"), ("subtitle-atkinson", "", "Atkinson Hyperlegible"),
        ("subtitle-barlow", "shadow", "Barlow Semi Condensed · shadow")])
    errors = (f'<h3>T1 · Field error</h3><div class="v-specimen app"><div class="ks-capture is-error" style="width:420px">{keys(["Tab"])}</div>'
              f'<p class="ks-msg error">{ic("block")}Tab moves between controls, so it can\'t be a shortcut. Press another key.</p></div>'
              '<p class="v-note">T2 (form summary) is not used: the player has no long forms.</p>'
              f'<h3>T3 · Snackbar</h3><div class="v-specimen app" style="position:relative;min-height:96px">{snack("Couldn&#39;t load Deutsch subtitles.", "Retry")}</div>'
              f'<h3>T4 · Callout</h3><div class="v-specimen app"><div class="callout info" style="max-width:560px" role="status">{ic("keyboard")}<div class="body"><strong>No keyboard connected</strong>These shortcuts work when you connect a keyboard to this device.</div></div></div>'
              f'<h3>T5 · Content can\'t be shown</h3><div class="v-specimen app" data-theme-panel="dark"><div class="sp-art" style="width:640px;display:grid;place-items:center">'
              + __import__("design.lib", fromlist=["error"]).error("failed") + '</div></div>'
              '<h3>T6 · Blocking dialog</h3><p>Not used in the player. Nothing here is costly or irreversible: resetting shortcuts and subtitle style has Undo, and your place is saved automatically.</p>')
    body = (f'<div class="v-section"><h2>Color</h2><p>The Marquee palette from the Details design, unchanged. Three roles were added for chrome over video: '
            'video, on-video and video-scrim-floor (the lightest point under text on a white frame).</p><div data-render="roles"></div></div>'
            '<div class="v-section"><h2>Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div></div>'
            '<div class="v-section"><h2>Type</h2><p>System font for every piece of UI. Subtitles are the one exception and use a bundled typeface (Nunito by default).</p>'
            '<div class="v-card" data-render="type" data-sample="Chapter 4 · The crossing · 24:16 of 52:04"></div></div>'
            '<div class="v-section"><h2>Spacing and shape</h2><div class="v-card" data-render="space"></div><div data-render="radius"></div><div data-render="elevation"></div></div>'
            f'<div class="v-section"><h2>Icons</h2><p>Material Symbols Rounded, filled.</p><div data-render="icons" data-names="{icon_names}"></div></div>'
            + section("c-btn", "Icon button", "Round tonal on its own; inside a pill the pill is the container. Every icon button has a label and, where one exists, its key.",
                      states_ + fig(plain, "inside a pill → transparent"), "48 × 48 px target, 8 px apart. Press turns the circle into a squircle.")
            + section("c-play", "Play", "The one filled control. Its shape tells paused from playing, so the state never depends on colour.", plays, "64 px in the dock, 56 px on phone landscape, 48 px in PiP.")
            + section("c-pill", "Pills and dock", "Opaque, so they read over any picture without a gradient.", pills, dark=True)
            + section("c-tl", "Timeline", "Timers left and right only. Chapter ticks ahead of the playhead. Slider role with a spoken value that includes the chapter.", tl, dark=True)
            + section("c-tip", "Tooltips and key caps", "The tooltip appears after 0.8 s on hover and at once on keyboard focus.", tips)
            + section("c-fb", "Shortcut feedback", "Status pill at the top centre of the picture.", fbk, dark=True)
            + section("c-menu", "Menu", "Opens upwards from its pill. On phone the same rows come as a bottom sheet.", menu_sp)
            + section("c-panel", "Panel", "Chapters and Up next. The list is one Tab stop; arrow keys move inside it.", panel_sp + rows)
            + section("c-next", "Up next card", "The only thing allowed over the windowed picture, and only in the last 10 seconds.", cards)
            + section("c-tiles", "Phone tiles", "Replace today's scrolling row of icons with labelled tiles.", tl_, dark=True)
            + f'<div class="v-section" id="c-subs"><h2>Subtitles</h2><p>Typeface, size and background are set on the Subtitle style page.</p><div class="sp-subs">{subs}</div></div>'
            + '<div class="v-section" id="errors"><h2>Error and feedback framework</h2><p>Decision order: needs a decision or is costly → T6; tied to one input → T1; content can\'t show → T5; lasting condition → T4; otherwise an event → T3.</p>'
            + errors + "</div>")
    return page("ds", "Design system", "Foundations",
                "Dockside: B's floating dock with C's heading and panel. The picture comes first, every control lives in an opaque pill, and every action has a key.", body)


def mode_map():
    nodes = [("Windowed", "Inside the app window. Panel beside the picture.", True), ("Fullscreen", "Edge to edge. Controls hide after 3 s.", False),
             ("PiP", "Always-on-top mini window.", False)]
    html = "".join(f'<div class="mm-node{" current" if c else ""}"><strong>{n}</strong><span>{t}</span></div>' for n, t, c in nodes)
    edges = [("Windowed → Fullscreen", ["F"], "or double-click the picture"), ("Fullscreen → Windowed", ["F", "Esc"], ""), ("Windowed → PiP", ["I"], ""),
             ("PiP → Windowed", ["Esc", "I"], "or Restore"), ("Fullscreen → PiP", ["I"], "leaves fullscreen first"), ("Windowed → closed", ["Esc"], "returns to Details")]
    html += '<div class="mm-edges">' + "".join(f'<div class="mm-edge"><strong>{a}</strong>{keys(k)}<span>{t}</span></div>' for a, k, t in edges) + "</div>"
    ladder = ('<div class="rules-wrap"><h2>Esc goes back one level</h2><table class="rules"><thead><tr><th>Order</th><th>If open</th><th>Esc does</th></tr></thead><tbody>'
              '<tr><td>1</td><td>Record dialog, cheat sheet, menu or sheet</td><td>Closes it</td></tr><tr><td>2</td><td>Up next countdown</td><td>Cancels it</td></tr>'
              '<tr><td>3</td><td>PiP</td><td>Restores the windowed player</td></tr><tr><td>4</td><td>Fullscreen</td><td>Returns to windowed</td></tr>'
              '<tr><td>5</td><td>Windowed player</td><td>Closes the player and saves your place</td></tr></tbody></table></div>')
    return app(f'<div class="pl-scroll"><div class="mm">{html}</div>{ladder}</div>')


def rules():
    rows = [("Controls hide", "Fullscreen and phone landscape: after 3 s while playing (longer with the system's accessibility timeout). Windowed, portrait and PiP-hover: never while the pointer or focus is inside."),
            ("Controls stay", "Paused, buffering, failed, a menu or sheet open, keyboard focus inside the controls, pointer resting on them, seeking."),
            ("Focus on open", "The player itself, not a control. Space is play / pause until Tab moves into the controls; then Space presses the focused control. K and the media key always play / pause."),
            ("Shortcuts while hidden", "Work without showing the controls; only the feedback pill appears. Tab shows the controls and focuses Back."),
            ("Menus", "Arrow keys move, Enter picks and closes, typing a letter jumps. Focus returns to the button that opened the menu."),
            ("Reduced motion", "Flat timeline, no shape morph, no feedback fade, static glow, no Up next auto-advance."),
            ("Screen reader", "Controls never hide. The timeline speaks time and chapter. Feedback pills and Up next are announced. No auto-advance."),
            ("Getting ready", "No blocking screen before playback: the player opens at once with the artwork and a spinner in place of Play."),
            ("Saving your place", "Closing returns at once. A small pill appears only if saving takes longer than 300 ms. Failure is a T3 with Retry on the screen you return to."),
            ("Narrow window", "Under 1100 px the panel is closed by default and opens over the picture. Under 940 px the volume slider folds into its button, Speed and PiP move into More.")]
    t = "".join(f"<tr><th>{a}</th><td>{b}</td></tr>" for a, b in rows)
    return app(f'<div class="pl-scroll"><div class="rules-wrap"><h2>Behaviour rules</h2><table class="rules"><tbody>{t}</tbody></table></div></div>')


def host(variant):
    pill_ = ""
    if variant == "saving":
        pill_ = f'<div class="host-pill"><div class="pl-feedback" role="status"><span class="spinner" style="width:20px;height:20px;border-width:2px"></span>Saving your place…</div></div>'
    if variant == "failed":
        pill_ = '<div class="snackbar-host" style="position:absolute;left:0;right:0;bottom:var(--space-6)"><div class="snackbar" role="status"><span class="msg">Couldn\'t save your place at 24:16.</span><button class="btn text">Retry</button></div></div>'
    return app(f'<div class="host"><div class="host-bar"><button class="icon-btn" aria-label="Back">{ic("arrow_back")}</button></div><div class="host-art">{art()}</div>'
               f'<div class="host-body"><span class="muted">Northbound · S2 · E5</span><h1>The Long Way Round</h1><div class="progress-bar"><i></i></div>'
               f'<p>Resume from 24:16 · 28 min left</p><div><button class="btn" data-goto="desktop/windowed/playing">{ic("play_arrow")}Resume from 24:16</button></div></div>{pill_}</div>')


def shell_page():
    s1 = [state("map", "Modes and Esc", "Any mode", mode_map(), "The three desktop modes never restart playback: the decoder keeps running while the window changes.")]
    s2 = [state("focus", "Tab order", "Tab from the player", windowed(fo=True), "Back → timeline → mute → volume → audio → subtitles → previous → back 10 → Play → forward 10 → next → speed → more → panel → PiP → fullscreen → panel list."),
          state("fullscreen", "Tab order · fullscreen", "Tab in fullscreen", fullscreen(fo=True, panel="chapters"), "Same order; the panel comes last.")]
    s3 = [state("rules", "Rules", "—", rules())]
    s4 = [state("saving", "Saving your place", "Player closed, save takes over 300 ms", host("saving")),
          state("failed", "Save failed", "Progress request failed", host("failed"), "T3 on the details screen. Replaces today's full-screen spinner and dialog."),
          state("done", "Back on Details", "Saved", host("done"), "Resume shows the new place at once.")]
    s5 = [state("t200", "Text at 200 %", "System font scale 200 %", portrait(t200=True, panel="none"),
                "Titles wrap, the timers move under the track, tiles go two-up. Nothing is cut off; the controls scroll if needed.")]
    body = "\n".join([
        screen("modes", "Modes", "How windowed, fullscreen and PiP connect, and what Esc does.", s1),
        screen("focus", "Focus order", "Numbered Tab order. Every stop has a visible focus ring.", s2),
        screen("behaviour", "Behaviour", "When controls hide, where focus starts, motion and screen readers.", s3),
        screen("host", "Closing the player", "What the user sees after Back or Esc.", s4),
        screen("text", "Large text", "200 % font scale on a phone.", s5, "f-fixed f-port"),
    ])
    return page("shell", "Shell and behaviour", "Foundations", "Rules that hold across every screen of the player.", body)
