"""Pieces shared by every page: icons, buttons, timeline, dock, panel, and the player frames built from them."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent  # docs/design/player
SRC = ROOT / "src"

SUB_LINE = "We never should have left the harbour."
KICKER, NAME = "Northbound · S2 · E5", "The Long Way Round"
MOVIE_KICKER, MOVIE_NAME = "2024 · 2 h 18 min", "The Salt Road"
LONG_KICKER = "Northbound: The Complete Director's Cut Collection · S12 · E104"
LONG_NAME = "The One Where Everything That Could Possibly Go Wrong on the Crossing Finally Does"
CHAPTERS = [("Cold open", "0:00", 0.0), ("Titles", "3:12", 6.1), ("Harbour", "4:40", 9.0), ("The crossing", "19:05", 36.7),
            ("Storm front", "33:30", 64.3), ("Landfall", "45:10", 86.7), ("Credits", "50:20", 96.6)]
EPISODES = [("S2 · E6 · 48 min", "Dead Reckoning", "Next", "night"), ("S2 · E7 · 51 min", "Slack Water", "", "dawn"),
            ("S2 · E8 · 58 min", "Home Light", "Season finale", "")]

_n = 0


def uid(prefix="x"):
    global _n
    _n += 1
    return f"{prefix}{_n}"


def esc(text):
    return str(text).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")


def art(variant=""):
    cls = ("pl-art " + variant).strip()
    return f'<div class="{cls}" aria-hidden="true"><div class="sun"></div><div class="wave"></div><div class="wave w2"></div><div class="boat"></div></div>'


def ic(name):
    return f'<i data-icon="{name}"></i>'


def kbd(combo):
    """One shortcut: 'Shift+N' becomes two key caps."""
    return "".join(f'<kbd class="kbd">{esc(k)}</kbd>' for k in combo.split("+"))


def keys(combos):
    return '<span class="keys">' + '<span class="or">or</span>'.join(kbd(c) for c in combos) + "</span>"


def fo(n):
    return f'<span class="fo" aria-hidden="true">{n}</span>' if n else ""


def btn(icon, label, cls="", key=None, tip=False, attrs="", n=None):
    ks = f' aria-keyshortcuts="{esc(key)}"' if key else ""
    tooltip = ""
    if tip:
        text = tip if isinstance(tip, str) else label
        cap = kbd(key) if key else ""
        tooltip = f'<span class="pl-tip" role="tooltip">{esc(text)}{cap}</span>'
    extra = f" {attrs}" if attrs else ""
    return f'<button class="{("pl-btn " + cls).strip()}" aria-label="{esc(label)}"{ks}{extra}>{ic(icon)}{tooltip}{fo(n)}</button>'


def go(route):
    return f'data-goto="{route}"'


def to(state_name):
    return f'data-state-to="{state_name}"'


# ---------------------------------------------------------------- picture
def pic(sub=True, lift=False, sub_cls="", art_cls="", line=SUB_LINE, extra=""):
    cls = ("pl-sub " + ("lift " if lift else "") + sub_cls).strip()
    text = f'<p class="{cls}"><span>{esc(line)}</span></p>' if sub else ""
    return f'<div class="pl-pic">{art(art_cls)}{text}{extra}</div>'


def video(kind, **kw):
    return f'<div class="pl-video {kind}">{pic(**kw)}</div>'


def title(kicker=KICKER, name=NAME, n=None):
    return (f'<div class="pl-title"><span class="pl-kicker truncate">{esc(kicker)}</span>'
            f'<h1 class="pl-name truncate" title="{esc(name)}">{esc(name)}</h1>{fo(n)}</div>')


def chapnow(text="Chapter 4 · The crossing"):
    return f'<span class="pl-chapnow">{ic("segment")}{esc(text)}</span>'


# ---------------------------------------------------------------- timeline, volume
def timeline(p=46.6, b=58, flat=False, focus=False, bubble=None, disabled=False, chapters=True, n=None, left="24:16", right="−27:48"):
    ticks = "".join(f'<span class="pl-chap" style="left:{pos}%"></span>' for _, _, pos in CHAPTERS[1:] if pos > p) if chapters and not disabled else ""
    if flat or disabled:
        played = '<span class="pl-played flat"></span>'
    else:
        wid = uid("wv")
        played = (f'<span class="pl-played"><svg aria-hidden="true"><defs><pattern id="{wid}" width="24" height="14" patternUnits="userSpaceOnUse">'
                  '<path d="M0 7 Q6 1 12 7 T24 7" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round"/></pattern></defs>'
                  f'<rect width="100%" height="14" fill="url(#{wid})"/></svg></span>')
    bub = f'<span class="pl-bubble" style="left:{bubble[0]}%"><b>{bubble[1]}</b>{esc(bubble[2])}</span>' if bubble else ""
    cls = "pl-track" + (" is-focus" if focus else "") + (" is-disabled" if disabled else "")
    dis = ' aria-disabled="true"' if disabled else ""
    return (f'<div class="pl-time"><span class="pl-t">{left}</span>'
            f'<div class="{cls}" role="slider" tabindex="0" aria-label="Playback position" aria-valuemin="0" aria-valuemax="3124" aria-valuenow="1456"{dis} '
            f'aria-valuetext="24 minutes 16 seconds of 52 minutes 4 seconds. Chapter 4, The crossing" style="--p:{p}%;--b:{b}%">'
            f'<span class="pl-rest"></span><span class="pl-buf"></span>{played}{ticks}<span class="pl-thumb"></span>{bub}{fo(n)}</div>'
            f'<button class="pl-t end" aria-label="27 minutes 48 seconds left. Switch to total duration">{right}</button></div>')


def vol(p=65, muted=False, n=None):
    return (f'<div class="pl-vol{" is-muted" if muted else ""}" role="slider" tabindex="0" aria-label="Volume" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{0 if muted else p}" '
            f'aria-valuetext="{"Muted" if muted else str(p) + " percent"}" style="--p:{p}%"><i></i>{fo(n)}</div>')


def pill(inner, cls=""):
    return f'<div class="{("pl-pill " + cls).strip()}">{inner}</div>'


SEP = '<span class="pl-sep" aria-hidden="true"></span>'
GROW = '<span class="grow"></span>'


def feedback(icon, text, meter=None, muted=False):
    bar = f'<span class="meter" style="--p:{meter}%"><i></i></span>' if meter is not None else ""
    return f'<div class="pl-feedback{" is-muted" if muted else ""}" role="status">{ic(icon)}<span>{esc(text)}</span>{bar}</div>'


def feedback_host(fb):
    return f'<div class="pl-feedback-host">{fb}</div>' if fb else ""


def snack(text, action=None, attrs="", top=False):
    act = f'<button class="btn text" {attrs}>{esc(action)}</button>' if action else ""
    pad = "" if action else ' style="padding-right:var(--space-4)"'
    return f'<div class="pl-snack{" top" if top else ""}"><div class="snackbar" role="status"{pad}><span class="msg">{esc(text)}</span>{act}</div></div>'


# ---------------------------------------------------------------- Up next and panel
def next_card(mode="pinned", routes=None):
    """pinned: in the panel. count: last 10 s with countdown. manual: no countdown (reduced motion or screen reader)."""
    r = routes or {}
    thumb = f'<div class="thumb">{art("night")}</div>'
    if mode == "pinned":
        return f'<div class="pl-next">{thumb}<div class="meta"><small>Up next · S2 · E6</small><strong class="truncate">Dead Reckoning</strong><small>48 min</small></div></div>'
    if mode == "count":
        acts = (f'<div class="acts"><span class="ring" aria-hidden="true"><i style="--p:80%"></i>8 s</span><button class="btn text" {r.get("cancel", "")}>Cancel</button>'
                f'<button class="btn" {r.get("next", "")}>Play now</button></div>')
        lead = "Up next in 8 seconds"
    else:
        acts = f'<div class="acts"><button class="btn text" {r.get("cancel", "")}>Close</button><button class="btn" {r.get("next", "")}>Play next episode</button></div>'
        lead = "Up next"
    return (f'<div class="pl-next float" role="group" aria-label="Up next">{thumb}<div class="meta"><small>{lead} · S2 · E6</small>'
            f'<strong class="truncate">Dead Reckoning</strong><small>48 min</small></div>{acts}</div>')


def chapter_rows(first=1, items=None, current=4):
    rows = ""
    for i, (name, at, _) in enumerate(items or CHAPTERS, start=1):
        if i < first:
            continue
        cur = i == current
        num = ic("play_arrow") if cur else str(i)
        label = f"Chapter {i}, {name}, starts at {at}" + (", playing" if cur else "")
        rows += (f'<li><button class="pl-chap-row" aria-label="{esc(label)}"{" aria-current=\"true\"" if cur else ""}>'
                 f'<span class="num">{num}</span><span class="truncate" title="{esc(name)}">{esc(name)}</span><span class="at">{at}</span></button></li>')
    return rows


def episode_rows(items=None):
    rows = ""
    for meta, name, tag, variant in (items or EPISODES):
        badge = f'<span class="tag">{tag}</span>' if tag else ""
        nxt = tag in ("Next", "Next season")
        rows += (f'<li><button class="pl-ep{" is-next" if nxt else ""}"><span class="thumb">{art(variant)}</span>'
                 f'<span class="meta"><small>{meta}</small><strong class="truncate">{esc(name)}</strong>{badge}</span></button></li>')
    return rows


MANY_CHAPTERS = [(n, f"{i * 3 // 60}:{i * 3 % 60:02d}:00" if i * 3 >= 60 else f"{i * 3}:00", 0.0) for i, n in enumerate([
    "Cold open", "Titles", "Harbour at first light and the argument about the tide tables", "Casting off", "The lighthouse keeper's warning",
    "Open water", "The crossing", "Radio silence", "Storm front", "Taking on water", "The long night watch below decks", "False dawn",
    "Landfall", "The customs shed", "What Marit knew all along", "Epilogue", "Credits", "Scene after the credits"])]


def side(tab="chapters", variant="", compact=False, routes=None, n=None):
    """variant: loading | failed | auto-off | next-season | finale | many | chapters-only | queue-only"""
    r = routes or {}
    chapters = tab == "chapters"
    if variant == "chapters-only":
        head = f'<div class="pl-side-title">{ic("segment")}Chapters</div>'
    elif variant == "queue-only":
        head = f'<div class="pl-side-title">{ic("playlist_play")}Up next</div>'
    else:
        head = (f'<div class="pl-tabs" role="tablist" aria-label="Chapters and up next">'
                f'<button role="tab" aria-selected="{str(chapters).lower()}" {r.get("tab_ch", "")}>{ic("segment")}Chapters</button>'
                f'<button role="tab" aria-selected="{str(not chapters).lower()}" {r.get("tab_up", "")}>{ic("playlist_play")}Up next</button>{fo(n)}</div>')
    auto = lambda on: f'<label class="switch pl-auto"><input type="checkbox"{" checked" if on else ""}><span>Play next automatically</span></label>'  # noqa: E731
    if variant == "loading":
        skel = "".join(f'<li><div class="row" style="min-height:48px;padding:0 var(--space-3);gap:var(--space-3)"><div class="skeleton circle" style="width:28px;height:28px"></div>'
                       f'<div class="skeleton line" style="flex:1;max-width:{w}%"></div><div class="skeleton line" style="width:36px"></div></div></li>' for w in (55, 40, 62, 70, 48, 58))
        body = f'<ol class="pl-list" aria-busy="true" aria-label="Loading chapters">{skel}</ol>'
    elif chapters and variant == "many":
        body = f'<ol class="pl-list" aria-label="Chapters">{chapter_rows(items=MANY_CHAPTERS, current=7)}</ol>' + next_card()
    elif chapters:
        body = f'<ol class="pl-list" aria-label="Chapters">{chapter_rows(first=3 if compact else 1)}</ol>' + ("" if compact or variant == "chapters-only" else next_card())
    elif variant == "failed":
        body = (f'<div class="pl-side-note grow" role="status">{ic("cloud_off")}<strong>Couldn\'t load what\'s next</strong>'
                f'<span>The server didn\'t respond. This episode keeps playing.</span><button class="btn tonal" {r.get("retry", "")}>Try again</button></div>')
    elif variant == "finale":
        body = (f'<div class="pl-side-note grow">{ic("flag")}<strong>Last episode</strong>'
                '<span>This is the final episode of Northbound. The player closes when it ends.</span></div>')
    elif variant == "next-season":
        items = [("S3 · E1 · 54 min", "Northern Lights", "Next season", "night"), ("S3 · E2 · 49 min", "The Ice Edge", "", "dawn")]
        body = f'<ol class="pl-list" aria-label="Up next">{episode_rows(items)}</ol>{auto(True)}'
    else:
        body = f'<ol class="pl-list" aria-label="Up next">{episode_rows()}</ol>{auto(variant != "auto-off")}'
    return f'<aside class="pl-side" aria-label="Chapters and up next">{head}{body}</aside>'


# ---------------------------------------------------------------- errors
def error(kind="failed", routes=None, android=False, compact=False):
    r = routes or {}
    if kind == "offline":
        icon, head, body = "cloud_off", "You're offline", "Connect to your server, or play something you downloaded."
        acts = f'<button class="btn" {r.get("downloads", "")}>Open downloads</button><button class="btn text" {r.get("retry", "")}>Try again</button>'
    else:
        icon, head = "error", "Couldn't play this video"
        body = "Check your connection, or the file may be in a format this device can't play. Your place at 24:16 is saved."
        acts = f'<button class="btn" {r.get("retry", "")}>Try again</button>'
        if android:
            acts += f'<button class="btn tonal" {r.get("external", "")}>Open in mpvEx</button>'
        acts += f'<button class="btn text" {r.get("back", "")}>Close player</button>'
    if compact:
        return (f'<div class="pl-error" role="alert">{ic(icon)}<h2>{head}</h2>'
                f'<div class="acts"><button class="btn" style="min-height:40px" {r.get("retry", "")}>Try again</button></div></div>')
    return f'<div class="pl-error" role="alert">{ic(icon)}<h2>{head}</h2><p>{body}</p><div class="acts">{acts}</div></div>'


# ---------------------------------------------------------------- dock
def transport(o):
    r = o["routes"]
    paused, dis = o["paused"], (" disabled" if o["failed"] else "")
    if o["buffering"] or o["opening"]:
        play = btn("pause", "Loading", "play is-buffering" + o["play_cls"], attrs=f'aria-busy="true" {r.get("play", "")}', n=o["fo"] and 9)
    else:
        play = btn("play_arrow" if paused else "pause", "Play" if paused else "Pause", "play" + (" is-paused" if paused else "") + o["play_cls"],
                   key="Space", attrs=(r.get("play", "") + dis).strip(), n=o["fo"] and 9)
    parts = [btn("replay_10", "Back 10 seconds", key="J", attrs=dis.strip(), n=o["fo"] and 8), play, btn("forward_10", "Forward 10 seconds", key="L", attrs=dis.strip(), n=o["fo"] and 10)]
    if not o["movie"]:
        parts = ([btn("skip_previous", "Previous episode", key="Shift+P", attrs=dis.strip(), n=o["fo"] and 7)] + parts
                 + [btn("skip_next", "Next episode", key="Shift+N", attrs=dis.strip(), n=o["fo"] and 11)])
    return "".join(parts)


def panel_btn(o):
    if o["panel"] == "none":
        return ""
    is_open = o["panel"] in ("chapters", "upnext")
    icon, label = ("right_panel_close", "Hide chapters and up next") if is_open else ("right_panel_open", "Show chapters and up next")
    return btn(icon, label, key="P", attrs=f'aria-pressed="{str(is_open).lower()}" {o["routes"].get("panel", "")}', n=o["fo"] and 14)


def dock(o):
    """Left: sound and language. Centre: transport. Right: playback and window. Both side pills stay the same width."""
    r, f, menu = o["routes"], o["fo"], o["menu"] or ("", "")
    dis = "disabled" if o["failed"] else ""
    exp = lambda name: f'aria-haspopup="menu" aria-expanded="{str(menu[0] == name).lower()}"'  # noqa: E731
    mute = btn("volume_off" if o["muted"] else "volume_up", "Unmute" if o["muted"] else "Mute", key="M", tip=o["tip"] == "mute",
               attrs=r.get("mute", ""), n=f and 3)
    if o["volpop"]:
        mute = btn("volume_up", "Volume, 65 percent", key="M", attrs='aria-expanded="true"') + (
            '<div class="pl-volpop" role="group" aria-label="Volume"><span>65</span><div class="bar" style="--p:65%"><i></i><b></b></div></div>')
    audio_attrs = f'{exp("audio")} {r.get("audio", "")} {dis}'.strip()
    if o["single_audio"]:
        audio = btn("music_note", "Audio track", "is-disabled", tip="Only one audio track", attrs='aria-disabled="true"')
    else:
        audio = btn("music_note", "Audio track", key="A", tip=o["tip"] == "audio", attrs=audio_attrs, n=f and 5)
    subs = btn("subtitles", "Subtitles", key="C", tip=o["tip"] == "subs", attrs=f'{exp("subs")} {r.get("subs", "")} {dis}'.strip(), n=f and 6)
    slider = "" if o["compact"] else vol(muted=o["muted"], n=f and 4)
    left = f'{mute}{slider}{"" if o["compact"] else SEP}{audio}{subs}' + (menu[1] if menu[0] in ("subs", "audio", "style") else "")
    speed = "" if o["compact"] else btn("speed", "Playback speed, 1×", tip=o["tip"] == "speed", attrs=f'{exp("speed")} {r.get("speed", "")} {dis}'.strip(), n=f and 12)
    more = btn("more_vert", "More options", attrs=f'{exp("more")} {r.get("more", "")}'.strip(), n=f and 13)
    pip_btn = "" if o["compact"] else btn("picture_in_picture_alt", "Picture in picture", key="I", attrs=r.get("pip", ""), n=f and 15)
    full = o["mode"] in ("full", "tab")
    fs = btn("fullscreen_exit" if full else "fullscreen", "Exit full screen" if full else "Full screen", key="F", tip=o["tip"] == "fs", attrs=r.get("fs", ""), n=f and 16)
    right = f'{speed}{more}{SEP}{panel_btn(o)}{pip_btn}{fs}' + (menu[1] if menu[0] in ("speed", "more") else "")
    return (f'<div class="pl-row"><div class="pl-group">{pill(left)}</div>{pill(transport(o), "dock")}'
            f'<div class="pl-group end">{pill(right)}</div></div>')


# ---------------------------------------------------------------- frames
DEFAULTS = dict(mode="win", paused=False, panel="chapters", side_variant="", kb=False, tip=None, menu=None, opening=False, buffering=False, failed=None,
                movie=False, kicker=KICKER, name=NAME, sub=True, sub_cls="", art_cls="", upnext=None, fb=None, snack="", flat=False, narrow=False, fo=False,
                muted=False, volpop=False, hidden=False, compact=False, single_audio=False, play_cls="", routes=None, overlay="", android=False, t200=False,
                locked=False, gesture="", chapters=True)


def opts(**kw):
    o = dict(DEFAULTS)
    unknown = set(kw) - set(o)
    assert not unknown, f"unknown options: {unknown}"
    o.update(kw)
    o["routes"] = dict(o["routes"] or {})
    if o["paused"]:
        o["flat"] = True
    return o


def app(inner, dark=False, overlay=""):
    panel = ' data-theme-panel="dark"' if dark else ""
    return f'<div class="app pl-app"{panel}>{inner}{overlay}</div>'


def head(o, n=False):
    r = o["routes"]
    back = btn("arrow_back", "Close player", key="Escape", attrs=r.get("back", ""), n=o["fo"] and 1)
    label = chapnow() if o["panel"] == "closed" and o["chapters"] else ""
    return f'<header class="pl-top">{back}{title(o["kicker"], o["name"])}{label}</header>'


def bottom(o):
    bubble = (55.0, "28:40", "The crossing") if o["kb"] else None
    line = timeline(flat=o["flat"], focus=o["kb"], bubble=bubble, disabled=bool(o["failed"] or o["opening"]), chapters=o["chapters"], n=o["fo"] and 2)
    return f'<footer class="pl-bottom">{pill(line, "line")}{dock(o)}</footer>'


def upnext_layer(o):
    if not o["upnext"]:
        return ""
    return f'<div class="pl-upnext">{next_card(o["upnext"], o["routes"])}</div>'


def boxed_picture(o):
    """The picture card used when nothing may cover the picture (windowed, portrait)."""
    if o["failed"]:
        return f'<div class="pl-video boxed failed" data-theme-panel="dark">{error(o["failed"], o["routes"], android=o["android"])}</div>'
    if o["opening"]:
        inner = pic(sub=False, art_cls="dim", extra='<div class="pl-center"><span class="pl-caption" role="status">Getting ready to play…</span></div>')
        return f'<div class="pl-video boxed">{inner}</div>'
    glow = f'<div class="pl-glow" aria-hidden="true">{art(o["art_cls"])}</div>'
    return glow + video("boxed", sub=o["sub"], sub_cls=o["sub_cls"], art_cls=o["art_cls"], extra=o["gesture"])


def side_for(o, compact=False):
    if o["panel"] not in ("chapters", "upnext"):
        return ""
    return side(o["panel"], o["side_variant"], compact=compact, routes=o["routes"], n=o["fo"] and 17)


def windowed(**kw):
    o = opts(mode="win", **kw)
    if o["narrow"]:
        o["compact"] = True
    main = (f'<div class="pl-main">{head(o)}<div class="pl-fit">{boxed_picture(o)}{feedback_host(o["fb"])}{upnext_layer(o)}</div>{bottom(o)}</div>')
    cls = "pl win" + (" narrow" if o["narrow"] else "")
    html = f'<div class="{cls}">{main}{side_for(o)}{o["snack"]}</div>'
    if o["narrow"]:
        return f'<div class="px-desk"><div class="px-window">{app(html, overlay=o["overlay"])}</div></div>'
    return app(html, overlay=o["overlay"])


def fullscreen(**kw):
    kw.setdefault("panel", "closed")
    mode = kw.pop("mode", "full")
    o = opts(mode=mode, **kw)
    paneled = o["panel"] in ("chapters", "upnext")
    cls = f'pl full{" tab" if mode == "tab" else ""}{" paneled" if paneled else ""}'
    if o["hidden"]:
        layer = feedback_host(o["fb"])
        return app(f'<div class="{cls}">{video("fill", sub=o["sub"], sub_cls=o["sub_cls"], art_cls=o["art_cls"], extra=o["gesture"])}{layer}{o["snack"]}</div>', dark=True, overlay=o["overlay"])
    if o["failed"]:
        picture = f'<div class="pl-video fill"></div><div class="pl-error-wrap">{error(o["failed"], o["routes"], android=o["android"])}</div>'
    elif o["opening"]:
        picture = video("fill", sub=False, art_cls="dim", extra='<div class="pl-center"><span class="pl-caption" role="status">Getting ready to play…</span></div>')
    else:
        picture = video("fill", sub=o["sub"], lift=True, sub_cls=o["sub_cls"], art_cls=o["art_cls"], extra=o["gesture"])
    main = f'<div class="pl-main">{head(o)}{GROW}{bottom(o)}</div>'
    return app(f'<div class="{cls}">{picture}<div class="pl-scrim"></div>{main}{side_for(o)}{feedback_host(o["fb"])}{upnext_layer(o)}{o["snack"]}</div>',
               dark=True, overlay=o["overlay"])


def pip(state="hover", **kw):
    """resting: picture only. hover: controls. paused. buffering. failed. upnext."""
    o = opts(mode="pip", **kw)
    r = o["routes"]
    top = pill(btn("open_in_full", "Restore player", "sm", key="Escape", attrs=r.get("restore", "")) + btn("close", "Close player", "sm", attrs=r.get("back", "")))
    edge = '<div class="pl-edge" aria-hidden="true"><i></i></div>'
    time_tag = '<div class="pl-pill tag pl-ptime"><span>24:16 / 52:04</span></div>'
    if state == "resting":
        return app(f'<div class="pl pip">{video("fill", sub=o["sub"])}{edge}</div>', dark=True)
    if state == "failed":
        body = f'<div class="pl-video fill"></div><header class="pl-top">{top}</header><div class="pl-dockwrap">{error("failed", r, compact=True)}</div>'
        return app(f'<div class="pl pip">{body}</div>', dark=True)
    if state == "min":
        play = btn("pause", "Pause", "play sm", key="Space")
        only = pill(btn("open_in_full", "Restore player", "sm", key="Escape", attrs=r.get("restore", "")))
        return app(f'<div class="pl pip">{video("fill", sub=False)}<header class="pl-top">{only}</header><div class="pl-dockwrap">{pill(play, "dock")}</div>{edge}</div>', dark=True)
    paused = state == "paused"
    if state == "buffering":
        play = btn("pause", "Loading", "play sm is-buffering", attrs='aria-busy="true"')
    else:
        play = btn("play_arrow" if paused else "pause", "Play" if paused else "Pause", "play sm" + (" is-paused" if paused else ""), key="Space", attrs=r.get("play", ""))
    mini = pill(btn("replay_10", "Back 10 seconds", "sm", key="J") + play + btn("forward_10", "Forward 10 seconds", "sm", key="L"), "dock")
    if state == "upnext":
        card = (f'<div class="pl-next float" role="group" aria-label="Up next"><div class="meta"><small>Up next in 8 s</small><strong class="truncate">Dead Reckoning</strong></div>'
                f'<button class="btn text" style="min-height:36px" {r.get("cancel", "")}>Cancel</button></div>')
        return app(f'<div class="pl pip">{video("fill", sub=False)}<header class="pl-top">{top}</header><div class="pl-dockwrap">{mini}</div>{card}{edge}</div>', dark=True)
    return app(f'<div class="pl pip">{video("fill", sub=False)}<header class="pl-top">{top}</header><div class="pl-dockwrap">{mini}</div>{time_tag}{edge}</div>', dark=True)


def tile(icon, label, value="", attrs=""):
    small = f"<small>{esc(value)}</small>" if value else ""
    return f'<button class="pl-tile" {attrs}>{ic(icon)}<span>{esc(label)}</span>{small}</button>'


def tiles(o):
    r = o["routes"]
    return ('<div class="pl-tiles">' + tile("subtitles", "Subtitles", "English", r.get("subs", "")) + tile("music_note", "Audio", "English 5.1", r.get("audio", ""))
            + tile("speed", "Speed", "1×", r.get("speed", "")) + tile("aspect_ratio", "Aspect", "Fit") + tile("lock", "Lock", attrs=r.get("lock", ""))
            + tile("screen_rotation", "Rotate", attrs=r.get("rotate", "")) + tile("picture_in_picture_alt", "PiP", attrs=r.get("pip", ""))
            + tile("more_horiz", "More", attrs=r.get("more", "")) + "</div>")


def unlock():
    return (f'<div class="pl-lockwrap"><div class="pl-unlock" role="slider" tabindex="0" aria-label="Slide to unlock controls" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">'
            f'<span class="thumb">{ic("lock")}</span><span class="hint">Slide to unlock{ic("chevron_right")}{ic("chevron_right")}</span></div></div>')


def seekfx(direction="fwd", seconds=10):
    icon, sign = ("fast_forward", "+") if direction == "fwd" else ("fast_rewind", "−")
    return f'<div class="pl-seekfx {direction}" role="status">{ic(icon)}<span>{sign}{seconds} s</span></div>'


def portrait(**kw):
    kw.setdefault("panel", "chapters")
    tabp = kw.pop("tabp", False)
    o = opts(mode="port", android=True, **kw)
    cls = "pl full port" + (" tabp" if tabp else "") + (" t200" if o["t200"] else "")
    show_side = o["panel"] in ("chapters", "upnext") and not o["t200"] and not o["failed"]
    fixed = f'<div class="pl-fit">{boxed_picture(o)}{feedback_host(o["fb"])}</div>'
    if o["locked"]:
        return app(f'<div class="{cls}"><div class="pl-main">{GROW}{fixed}{GROW}</div>{unlock()}</div>', dark=True)
    if o["failed"]:
        picture, spacer = f'<div class="pl-error-wrap">{error(o["failed"], o["routes"], android=True)}</div>', GROW
        o["panel"] = "none"
    elif show_side:
        picture, spacer = fixed, ""
    elif o["t200"] or o["upnext"]:
        picture, spacer = fixed, GROW
    else:  # nothing to list: the picture centres in the free space
        picture, spacer = f'<div class="pl-fit free">{boxed_picture(o)}{feedback_host(o["fb"])}</div>', ""
    line = pill(timeline(flat=o["flat"], disabled=bool(o["failed"] or o["opening"]), chapters=o["chapters"]), "line")
    if tabp:
        left = pill(btn("lock", "Lock controls") + btn("screen_rotation", "Rotate screen") + btn("speed", "Playback speed, 1×"))
        right = pill(btn("subtitles", "Subtitles", key="C") + btn("music_note", "Audio track", key="A") + btn("picture_in_picture_alt", "Picture in picture") + btn("more_vert", "More options"))
        controls = f'<div class="pl-row"><div class="pl-group">{left}</div>{pill(transport(o), "dock")}<div class="pl-group end">{right}</div></div>'
    else:
        controls = pill(transport(o), "dock") + tiles(o)
    up = f'<div class="pl-upnext">{next_card(o["upnext"], o["routes"])}</div>' if o["upnext"] else ""
    middle = up + (side_for(o, compact=not tabp) if show_side else "")
    main = f'<div class="pl-main">{head(o)}{picture}{middle}{spacer}<footer class="pl-bottom">{line}{controls}</footer></div>'
    return app(f'<div class="{cls}">{main}{o["snack"]}</div>', dark=True, overlay=o["overlay"])


def landscape(**kw):
    kw.setdefault("panel", "closed")
    o = opts(mode="land", android=True, play_cls=" md", **kw)
    paneled = o["panel"] in ("chapters", "upnext")
    cls = "pl full land" + (" paneled" if paneled else "")
    r = o["routes"]
    if o["locked"]:
        return app(f'<div class="{cls}">{video("fill", sub=o["sub"])}{unlock()}</div>', dark=True)
    if o["hidden"]:
        return app(f'<div class="{cls}">{video("fill", sub=o["sub"], extra=o["gesture"])}{feedback_host(o["fb"])}</div>', dark=True)
    if o["failed"]:
        picture = f'<div class="pl-video fill"></div><div class="pl-error-wrap">{error(o["failed"], r, android=True)}</div>'
        return app(f'<div class="{cls}">{picture}<div class="pl-main">{head(o)}{GROW}</div></div>', dark=True)
    left = pill(btn("lock", "Lock controls", attrs=r.get("lock", "")) + btn("screen_rotation", "Rotate screen") + btn("speed", "Playback speed, 1×"))
    right = pill(btn("subtitles", "Subtitles", key="C", attrs=r.get("subs", "")) + btn("music_note", "Audio track", key="A") + panel_btn(o) + btn("more_vert", "More options"))
    line = pill(timeline(flat=o["flat"]), "line")
    row = f'<div class="pl-row"><div class="pl-group">{left}</div>{pill(transport(o), "dock")}<div class="pl-group end">{right}</div></div>'
    main = f'<div class="pl-main">{head(o)}{GROW}<footer class="pl-bottom">{line}{row}</footer></div>'
    return app(f'<div class="{cls}">{video("fill", sub=o["sub"] and paneled, extra=o["gesture"])}<div class="pl-scrim"></div>{main}{side_for(o, compact=True)}{upnext_layer(o)}{o["snack"]}</div>',
               dark=True, overlay=o["overlay"])


# ---------------------------------------------------------------- viewer markup
def state(key, label, trigger, html, note=""):
    extra = f' data-note="{esc(note)}"' if note else ""
    return f'<div class="v-state" data-state="{key}" data-label="{esc(label)}" data-trigger="{esc(trigger)}"{extra}>{html}</div>'


def screen(key, heading, purpose, states, cls="f-fixed"):
    return (f'<article class="v-screen {cls}" data-screen="{key}" data-title="{esc(heading)}" data-purpose="{esc(purpose)}">\n'
            + "\n".join(states) + "\n</article>")


def page(key, heading, group, lead, body):
    return (f'<section class="v-page" data-page="{key}" data-title="{esc(heading)}" data-group="{esc(group)}">\n<h1>{esc(heading)}</h1>\n'
            f'<p class="v-lead">{lead}</p>\n{body}\n</section>')
