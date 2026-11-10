"""Keyboard: feedback catalogue, cheat sheet, Settings → Keyboard shortcuts, record dialog."""
from .lib import app, esc, feedback, fullscreen, go, ic, kbd, keys, page, screen, state, to, video, windowed

GROUPS = [
    ("Playback", [("Play or pause", ["Space", "K"]), ("Next episode", ["Shift+N"]), ("Previous episode", ["Shift+P"]), ("Slower / faster", ["<", ">"])]),
    ("Seeking", [("Back / forward 5 s", ["←", "→"]), ("Back / forward 10 s", ["J", "L"]), ("Previous / next chapter", ["Ctrl+←", "Ctrl+→"]),
                 ("Jump to 0–90 %", ["0 … 9"]), ("Jump to start", ["Home"])]),
    ("Volume", [("Volume up / down", ["↑", "↓"]), ("Mute", ["M"])]),
    ("Tracks and display", [("Subtitles on or off", ["C"]), ("Subtitles menu", ["S"]), ("Audio menu", ["A"]), ("Cycle aspect ratio", ["Z"]),
                            ("Chapters and up next panel", ["P"])]),
    ("Window", [("Full screen", ["F"]), ("Picture in picture", ["I"]), ("Back one level", ["Esc"])]),
    ("Help", [("Show shortcuts", ["?"])]),
]
GENERAL = ("General", [("Refresh Home", ["Ctrl+R"])])


def app_scroll(inner):
    """Settings scroll inside the frame; the top bar sticks."""
    return app(f'<div class="pl-scroll">{inner}</div>')


def cheat(custom=False, single=False, r=None):
    r = r or {}
    out = ""
    for name, rows in GROUPS:
        out += f'<section class="cs-group"><h3>{name}</h3>'
        for label, combos in rows:
            tag = ""
            if custom and label == "Mute":
                combos, tag = ["U"], '<span class="tag">Changed</span>'
            out += f'<div class="cs-row"><span>{esc(label)} {tag}</span>{keys(combos)}</div>'
        out += "</section>"
    return (f'<div class="overlay" data-overlay data-static><div class="cs{" single" if single else ""}" role="dialog" aria-modal="true" aria-labelledby="cs-t">'
            f'<div class="cs-head"><h2 id="cs-t">Keyboard shortcuts</h2><button class="icon-btn" aria-label="Close" {r.get("close", "")}>{ic("close")}</button></div>'
            f'<div class="cs-body">{out}</div><div class="cs-foot"><span>Space works on the player; on a focused button it presses the button. K always plays or pauses.</span>'
            f'<button class="btn tonal" {r.get("custom", "")}>Customise</button></div></div></div>')


def ks_row(label, combos, sub="", changed=False, fixed=False, none=False, reset_attrs="", edit_attrs="", hl=""):
    name = esc(label)
    if hl:
        name = name.replace(hl, f"<mark>{hl}</mark>", 1)
    second = f"<small>{esc(sub)}</small>" if sub else ""
    tag = '<span class="tag">Changed</span>' if changed else ""
    if fixed:
        key_html = f'<span class="ks-key fixed" aria-label="{esc(combos[0])}, can\'t be changed">{keys(combos)}{ic("lock")}</span>'
    elif none:
        key_html = f'<button class="ks-key none" {edit_attrs}>Not set</button>'
    else:
        key_html = f'<button class="ks-key" aria-label="Change shortcut for {name}, now {esc(" or ".join(combos))}" {edit_attrs}>{keys(combos)}</button>'
    reset = f'<button class="icon-btn" aria-label="Reset {name} to default" {reset_attrs}>{ic("restart_alt")}</button>' if changed else ""
    return f'<li class="ks-row"><span class="name"><span>{name} {tag}</span>{second}</span>{key_html}{reset}</li>'


def settings(variant="default", r=None):
    r = r or {}
    edit = r.get("edit", "")
    q = "mute" if variant in ("search", "empty") else ""
    if variant == "empty":
        q = "rewind"
    search = (f'<div class="field"><label for="ks-q">Search shortcuts</label><div class="control">{ic("search")}'
              f'<input id="ks-q" value="{q}" placeholder="Action or key, e.g. Mute or M"></div></div>'
              f'<button class="btn text" {r.get("reset_all", "")}>Reset all</button>')
    callout = ""
    if variant == "nokbd":
        callout = (f'<div class="callout info" role="status">{ic("keyboard")}<div class="body"><strong>No keyboard connected</strong>'
                   'These shortcuts work when you connect a keyboard to this device. You can still change them now.</div></div>')
    groups = ""
    if variant == "empty":
        groups = (f'<div class="ks-empty">{ic("search_off")}<strong>No shortcut matches "rewind"</strong><span>Try "back" or "seek", or a key such as J.</span>'
                  f'<button class="btn text" {r.get("clear", "")}>Clear search</button></div>')
    elif variant == "search":
        groups = (f'<div class="ks-group"><h2>Volume</h2><ul class="ks-list">{ks_row("Mute", ["M"], "Turns sound off and back on", hl="Mute", edit_attrs=edit)}</ul></div>'
                  '<p class="muted" style="margin:0 var(--space-4)">1 shortcut matches "mute".</p>')
    else:
        custom = variant in ("custom", "reset-one")
        for name, rows in GROUPS + [GENERAL]:
            items = ""
            for label, combos in rows:
                changed = custom and label == "Mute" and variant == "custom"
                c = ["U"] if changed else combos
                items += ks_row(label, c, changed=changed, fixed=label == "Back one level", reset_attrs=r.get("reset_one", ""), edit_attrs=edit,
                                none=custom and label == "Cycle aspect ratio")
            groups += f'<div class="ks-group"><h2>{name}</h2><ul class="ks-list">{items}</ul></div>'
    snackbar = ""
    if variant == "reset-one":
        snackbar = '<div class="snackbar-host"><div class="snackbar" role="status"><span class="msg">Mute reset to M.</span><button class="btn text">Undo</button></div></div>'
    if variant == "reset-all":
        snackbar = '<div class="snackbar-host"><div class="snackbar" role="status"><span class="msg">All 26 shortcuts reset to their defaults.</span><button class="btn text">Undo</button></div></div>'
    return (f'<header class="topbar"><button class="icon-btn" aria-label="Back" {r.get("back", "")}>{ic("arrow_back")}</button><span class="title">Keyboard shortcuts</span></header>'
            f'<div class="ks">{callout}<div class="ks-tools">{search}</div>{groups}</div>{snackbar}')


def record(variant="listening", r=None):
    r = r or {}
    if variant == "listening":
        cap, msg = '<span>Press the new keys</span>', f'<p class="ks-msg">{ic("info")}Hold Ctrl, Shift or Alt with another key to make a combination. Esc cancels.</p>'
    elif variant == "captured":
        cap, msg = keys(["U"]), f'<p class="ks-msg">{ic("check_circle")}U is free.</p>'
    elif variant == "conflict":
        cap = keys(["Ctrl+R"])
        msg = f'<p class="ks-msg error" role="alert">{ic("warning")}Ctrl+R is used by Refresh Home. Replace it, and Refresh Home will have no shortcut.</p>'
    elif variant == "reserved":
        cap, msg = keys(["Tab"]), f'<p class="ks-msg error" role="alert">{ic("block")}Tab moves between controls, so it can\'t be a shortcut. Press another key.</p>'
    else:
        cap, msg = keys(["Ctrl"]), f'<p class="ks-msg">{ic("info")}Now add a key, for example Ctrl+M.</p>'
    err = " is-error" if variant in ("conflict", "reserved") else ""
    if variant == "conflict":
        acts = f'<button class="btn text lead" {r.get("again", "")}>Try another</button><button class="btn text" {r.get("cancel", "")}>Cancel</button><button class="btn" {r.get("save", "")}>Replace</button>'
    else:
        dis = "" if variant == "captured" else " disabled"
        acts = f'<button class="btn text lead">Reset to M</button><button class="btn text" {r.get("cancel", "")}>Cancel</button><button class="btn" {r.get("save", "")}{dis}>Save</button>'
    dlg = (f'<div class="overlay" data-overlay data-static><div class="dialog" role="dialog" aria-modal="true" aria-labelledby="rec-t">'
           f'<h2 class="ks-title" id="rec-t">Shortcut for Mute</h2><p>Now: {kbd("M")}</p>'
           f'<div class="ks-capture{err}" aria-live="polite">{cap}</div>{msg}<div class="actions">{acts}</div></div></div>')
    return app(f'<div class="pl-scroll">{settings("default")}</div>' + dlg)


FB = [
    ("play_arrow", "Play", ["Space", "K"]), ("pause", "Pause", ["Space", "K"]), ("replay_5", "−5 s", ["←"]), ("forward_10", "+10 s", ["L"]),
    ("skip_next", "Chapter 5 · Storm front", ["Ctrl+→"]), ("move_down", "Jumped to 50 % · 26:02", ["5"]), ("volume_up", "65 %", ["↑"]),
    ("volume_off", "Muted", ["M"]), ("speed", "1.25×", [">"]), ("subtitles", "Subtitles on · English", ["C"]), ("subtitles_off", "Subtitles off", ["C"]),
    ("aspect_ratio", "Crop", ["Z"]), ("skip_next", "Next episode · Dead Reckoning", ["Shift+N"]), ("block", "No previous episode", ["Shift+P"]),
]


def catalogue():
    items = ""
    for icon, text, combos in FB:
        meter = 65 if text == "65 %" else None
        items += f'<figure>{feedback(icon, text, meter=meter, muted=text in ("Muted", "No previous episode"))}<figcaption>{keys(combos)}</figcaption></figure>'
    return app(f'<div class="pl full">{video("fill", sub=False, art_cls="dim")}<div class="fb-grid">{items}</div></div>', dark=True)


def keyboard_page():
    open_settings = go("keyboard/settings/default")
    fb = [state("catalogue", "Every feedback pill", "Each shortcut", catalogue(),
                "One pill at the top centre, about 1.3 s, then it fades (no fade with reduced motion). A key that does nothing here still answers, so the user knows it was heard.")]
    cs_r = dict(close=go("desktop/windowed/playing"), custom=open_settings)
    cs = [
        state("default", "Cheat sheet", "? pressed, or More → Keyboard shortcuts", windowed(paused=True, overlay=cheat(r=cs_r)),
              "Playback pauses while the sheet is open and resumes on close. Esc or ? closes it."),
        state("custom", "With a changed key", "Mute rebound to U", windowed(paused=True, overlay=cheat(custom=True, r=cs_r)),
              "Changed keys are tagged, so the sheet never shows a default that no longer works."),
        state("single", "Narrow / tablet", "Window under 940 px, or tablet with a keyboard", fullscreen(mode="tab", paused=True, overlay=cheat(single=True, r=cs_r)),
              "One column that scrolls."),
    ]
    sr = dict(edit=go("keyboard/record/listening"), reset_all=to("reset-all"), reset_one=to("reset-one"), back=go("keyboard/cheatsheet/default"), clear=to("default"))
    se = [
        state("default", "All shortcuts", "Settings → Keyboard shortcuts", app_scroll(settings("default", sr)),
              "Grouped like the cheat sheet. Each key cap is the button that changes it. Esc is fixed and shows a lock."),
        state("search", "Search", "Typing in search", app_scroll(settings("search", sr)), "Matches action names and keys alike."),
        state("empty", "No results", "Nothing matches", app_scroll(settings("empty", sr))),
        state("custom", "Changed rows", "After rebinding", app_scroll(settings("custom", sr)), "Changed rows get a tag and a reset button; an unset action says Not set."),
        state("reset-one", "Shortcut reset", "Reset on one row", app_scroll(settings("reset-one", sr)), "T3 with Undo: restoring is cheap, so no confirmation."),
        state("reset-all", "All reset", "Reset all", app_scroll(settings("reset-all", sr)), "T3 with Undo, kept on screen for 10 s."),
        state("nokbd", "No keyboard (Android)", "Android with no hardware keyboard", app_scroll(settings("nokbd", sr)), "T4 info: lasts while no keyboard is connected."),
    ]
    rr = dict(cancel=go("keyboard/settings/default"), save=go("keyboard/settings/custom"), again=to("listening"))
    rec = [
        state("listening", "Listening", "Key cap pressed", record("listening", rr), "Focus is in the capture box; every key goes there except Esc, which cancels."),
        state("captured", "Captured", "U pressed", record("captured", rr)),
        state("conflict", "Conflict", "Ctrl+R pressed", record("conflict", rr), "T1: belongs to this one input, so it is said here, with both ways out."),
        state("reserved", "Reserved key", "Tab pressed", record("reserved", rr), "Esc, Tab, Shift+Tab and Enter are reserved."),
        state("modifier", "Modifier only", "Ctrl held, nothing else yet", record("modifier", rr)),
    ]
    body = "\n".join([
        screen("feedback", "Shortcut feedback", "Answers 'did my key do something?' without showing the controls.", fb),
        screen("cheatsheet", "Cheat sheet", "Learn the keys from inside the player.", cs),
        screen("settings", "Settings · Keyboard shortcuts", "Change a shortcut. Primary action: press a key cap.", se),
        screen("record", "Record a shortcut", "Press the new keys.", rec),
    ])
    return page("keyboard", "Keyboard", "Keyboard",
                "Every player action has a key, every key gives feedback, every tooltip names its key, and every key can be changed in Settings.", body)
