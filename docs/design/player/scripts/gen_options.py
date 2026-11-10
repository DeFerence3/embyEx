#!/usr/bin/env python3
"""Generates round 1 of the player directions: three token files and options/round-1/src/index.html.
Run from anywhere:  python3 docs/design/player/scripts/gen_options.py
Every frame is assembled from the same pieces so the three directions stay comparable; edit pieces here, not the generated HTML."""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # docs/design/player
BASE = json.loads((ROOT.parent / "tokens.json").read_text())  # Marquee tokens of the Details design (brand, fixed)
OUT = ROOT / "options" / "round-1"

SUB_LINE = "We never should have left the harbour."
ART = '<div class="pl-art" aria-hidden="true"><div class="sun"></div><div class="wave"></div><div class="wave w2"></div><div class="boat"></div></div>'
CHAPTERS = [("Cold open", "0:00", 0.0), ("Titles", "3:12", 6.1), ("Harbour", "4:40", 9.0), ("The crossing", "19:05", 36.7),
            ("Storm front", "33:30", 64.3), ("Landfall", "45:10", 86.7), ("Credits", "50:20", 96.6)]


# ---------------------------------------------------------------- tokens
def make_tokens(name, rationale, subtitle_font, subtitle_type, timer_type, sizes):
    t = copy.deepcopy(BASE)
    t["meta"]["name"] = name
    t["meta"]["rationale"] = rationale
    # Video chrome: identical in both themes, like PlayerColors in the app.
    t["primitives"]["color"]["video"] = {"black": "#000000", "floor": "#5c5c5c", "white": "#ffffff"}
    for theme in ("light", "dark"):
        t["color"][theme].update({"video": "{video.black}", "on-video": "{video.white}", "video-scrim-floor": "{video.floor}"})
    t["contrast"] += [
        {"fg": "on-video", "bg": ["video-scrim-floor"], "use": "text"},
        {"fg": "primary", "bg": ["video", "surface-container-high"], "use": "ui"},
        {"fg": "on-surface-variant", "bg": ["surface-container-highest"], "use": "text"},
        {"fg": "outline", "bg": ["surface-container-highest", "surface-container-high"], "use": "ui"},
    ]
    t["font"]["subtitle"] = subtitle_font
    t["font"]["mono"] = {"family": "ui-monospace, 'Cascadia Mono', 'Roboto Mono', 'DejaVu Sans Mono'", "fallback": "monospace"}
    t["type"]["subtitle"] = dict(subtitle_type, font="subtitle", sample=SUB_LINE)
    t["type"]["timer"] = dict(timer_type, sample="24:16  −27:48  1:52:04")
    t["size"].update(sizes)
    return t


TOKENS = {
    "a": make_tokens(
        "Centre Stage",
        "Today's overlay with the timeline freed. The safe choice: nothing to relearn in fullscreen, windowed mode moves the controls under the picture.",
        {"family": "Atkinson Hyperlegible", "fallback": "system-ui, sans-serif", "google": "Atkinson Hyperlegible:wght@400;700"},
        {"size": 28, "line": 38, "weight": 700},
        {"font": "mono", "size": 13, "line": 16, "weight": 500},
        {"control": 48, "touch": 48, "play": 80, "play-row": 56},
    ),
    "b": make_tokens(
        "Dock",
        "One floating dock in every mode. Opaque pills carry all controls, so the picture needs no scrim.",
        {"family": "Nunito", "fallback": "system-ui, sans-serif", "google": "Nunito:wght@600;700;800"},
        {"size": 28, "line": 38, "weight": 700},
        {"font": "body", "size": 14, "line": 20, "weight": 600, "tracking": 0.2},
        {"control": 48, "touch": 48, "play": 64, "play-row": 64},
    ),
    "c": make_tokens(
        "Console",
        "A desk player: labelled values in an opaque console, and a chapters panel in the windowed margin.",
        {"family": "Barlow Semi Condensed", "fallback": "system-ui, sans-serif", "google": "Barlow Semi Condensed:wght@500;600"},
        {"size": 30, "line": 38, "weight": 600, "tracking": 0.2},
        {"font": "body", "size": 14, "line": 20, "weight": 600, "tracking": 0.2},
        {"control": 40, "touch": 48, "play": 48, "play-row": 48},
    ),
}


# ---------------------------------------------------------------- shared pieces
def ic(name):
    return f'<i data-icon="{name}"></i>'


def btn(icon, label, cls="", key=None, tip=False):
    keys = f' aria-keyshortcuts="{key}"' if key else ""
    tooltip = ""
    if tip:
        cap = f'<kbd class="kbd">{key}</kbd>' if key else ""
        tooltip = f'<span class="pl-tip" role="tooltip">{label}{cap}</span>'
    return f'<button class="{("pl-btn " + cls).strip()}" aria-label="{label}"{keys}>{ic(icon)}{tooltip}</button>'


def pic(sub=True, lift=False, outline=False):
    cls = "pl-sub" + (" lift" if lift else "") + (" outline" if outline else "")
    line = f'<p class="{cls}"><span>{SUB_LINE}</span></p>' if sub else ""
    return f'<div class="pl-pic">{ART}{line}</div>'


def video(kind, **kw):
    return f'<div class="pl-video {kind}">{pic(**kw)}</div>'


def title(one_line=None):
    if one_line:
        return f'<div class="pl-title grow"><h1 class="pl-name truncate">{one_line}</h1></div>'
    return ('<div class="pl-title grow"><span class="pl-kicker truncate">Northbound · S2 · E5</span>'
            '<h1 class="pl-name truncate">The Long Way Round</h1></div>')


_wave = 0


def timeline(opt, p=46.6, b=58, paused=False, focus=False, bubble=None, thin=False):
    global _wave
    ticks = "" if thin else "".join(
        f'<span class="pl-chap{" done" if pos < p else ""}" style="left:{pos}%"></span>' for _, _, pos in CHAPTERS[1:])
    played = '<span class="pl-played"></span>'
    if opt == "b":
        if paused or thin:
            played = '<span class="pl-played flat"></span>'
        else:
            _wave += 1
            played = (f'<span class="pl-played"><svg aria-hidden="true"><defs><pattern id="wv{_wave}" width="24" height="14" patternUnits="userSpaceOnUse">'
                      '<path d="M0 7 Q6 1 12 7 T24 7" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round"/></pattern></defs>'
                      f'<rect width="100%" height="14" fill="url(#wv{_wave})"/></svg></span>')
    bub = f'<span class="pl-bubble" style="left:{bubble[0]}%"><b>{bubble[1]}</b>{bubble[2]}</span>' if bubble else ""
    cls = "pl-track" + (" is-focus" if focus else "")
    return (f'<div class="pl-time{" thin" if thin else ""}"><span class="pl-t">24:16</span>'
            f'<div class="{cls}" role="slider" tabindex="0" aria-label="Playback position" aria-valuemin="0" aria-valuemax="3124" aria-valuenow="1456" '
            f'aria-valuetext="24 minutes 16 seconds of 52 minutes 4 seconds. Chapter 4, The crossing" style="--p:{p}%;--b:{b}%">'
            f'<span class="pl-rest"></span><span class="pl-buf"></span>{played}{ticks}<span class="pl-thumb"></span>{bub}</div>'
            '<button class="pl-t end" aria-label="27 minutes 48 seconds left. Switch to total duration">−27:48</button></div>')


def vol():
    return ('<div class="pl-vol" role="slider" tabindex="0" aria-label="Volume" aria-valuemin="0" aria-valuemax="100" aria-valuenow="65" '
            'aria-valuetext="65 percent" style="--p:65%"><i></i></div>')


def transport(paused=False, play_cls="row", prevnext=True, side_cls=""):
    play = btn("play_arrow" if paused else "pause", "Play" if paused else "Pause",
               f"play {play_cls}" + (" is-paused" if paused else ""), key="Space")
    parts = [btn("replay_10", "Back 10 seconds", side_cls, key="J"), play, btn("forward_10", "Forward 10 seconds", side_cls, key="L")]
    if prevnext:
        parts = [btn("skip_previous", "Previous episode", side_cls, key="Shift+P")] + parts + [btn("skip_next", "Next episode", side_cls, key="Shift+N")]
    return "".join(parts)


BACK = btn("arrow_back", "Close player", key="Escape")
MUTE = btn("volume_up", "Mute", key="M")
SUBS = btn("subtitles", "Subtitles", key="C")
SUBS_TIP = btn("subtitles", "Subtitles", "is-hover", key="C", tip=True)
AUDIO = btn("music_note", "Audio track", key="A")
SPEED = btn("speed", "Playback speed, 1×")
ASPECT = btn("aspect_ratio", "Aspect ratio, Fit", key="Z")
MORE = btn("more_vert", "More options")
PIP = btn("picture_in_picture_alt", "Picture in picture", key="I")
FS = btn("fullscreen", "Full screen", key="F")
FS_EXIT = btn("fullscreen_exit", "Exit full screen", key="F")
LOCK = btn("lock", "Lock controls")
ROTATE = btn("screen_rotation", "Rotate screen")
SEP = '<span class="pl-sep" aria-hidden="true"></span>'
GROW = '<span class="grow"></span>'
RESTORE = btn("open_in_full", "Restore player", "sm", key="Escape")
CLOSE = btn("close", "Close player", "sm")


def bezel(icon, text, meter=None, static=False):
    bar = f'<span class="meter" style="--p:{meter}%"><i></i></span>' if meter is not None else ""
    return f'<div class="pl-bezel{" static" if static else ""}" role="status">{ic(icon)}<span>{text}</span>{bar}</div>'


def next_card(actions=False):
    acts = ('<div class="acts"><button class="btn text">Cancel</button><button class="btn">Play now</button></div>' if actions else "")
    lead = "Up next in 8 s" if actions else "Up next"
    return (f'<div class="pl-next"><div class="thumb">{ART}</div><div class="meta"><small>{lead} · S2 · E6</small>'
            f'<strong class="truncate">Dead Reckoning</strong><small>48 min</small></div>{acts}</div>')


def app(inner, dark=False):
    panel = ' data-theme-panel="dark"' if dark else ""
    return f'<div class="app pl-app"{panel}>{inner}</div>'


def frame(kind, inner, theme=None, zoom=""):
    panel = f' data-theme-panel="{theme}"' if theme else ""
    return f'<div class="v-frame pf pf-{kind} {zoom}"{panel}>{inner}</div>'


def cell(label, html):
    return f'<div class="px-cell"><p class="v-label">{label}</p>{html}</div>'


# ---------------------------------------------------------------- A · Centre Stage
def a_right(full, tip=False):
    return f'<div class="pl-group end">{SUBS_TIP if tip else SUBS}{AUDIO}{SPEED}{MORE}{SEP}{PIP}{FS_EXIT if full else FS}</div>'


def a_full():
    return app(f'''<div class="pl full">{video("fill", lift=True)}<div class="pl-scrim"></div>
<header class="pl-top">{BACK}{title()}</header>
<div class="pl-center">{transport(play_cls="")}</div>
<footer class="pl-bottom">{timeline("a")}
<div class="pl-row split"><div class="pl-group">{MUTE}{vol()}</div>{a_right(True)}</div></footer></div>''', dark=True)


def a_win(paused=False, kb=False, tip=False):
    bubble = (55.0, "28:40", "The crossing") if kb else None
    return app(f'''<div class="pl win"><header class="pl-top">{BACK}{title()}</header>
<div class="pl-fit">{video("boxed")}</div>
<footer class="pl-deck">{timeline("a", paused=paused, focus=kb, bubble=bubble)}
<div class="pl-row"><div class="pl-group">{MUTE}{vol()}</div><div class="pl-group mid">{transport(paused=paused)}</div>{a_right(False, tip)}</div></footer></div>''')


def a_pip():
    return app(f'''<div class="pl full pip">{video("fill", sub=False)}<div class="pl-scrim"></div>
<header class="pl-top">{title("Northbound · S2 · E5 · The Long Way Round")}{RESTORE}{CLOSE}</header>{GROW}
<footer class="pl-bottom">{timeline("a", thin=True)}<div class="pl-group mid">{transport(prevnext=False, side_cls="sm")}</div></footer></div>''', dark=True)


def a_port():
    return app(f'''<div class="pl full port">{video("fill")}<div class="pl-scrim"></div>
<header class="pl-top">{BACK}{title()}</header>{GROW}
<footer class="pl-bottom">{timeline("a")}<div class="pl-group mid">{transport(play_cls="")}</div>
<div class="pl-quick">{ROTATE}{AUDIO}{SUBS}{SPEED}{LOCK}{PIP}{ASPECT}{MORE}</div></footer></div>''', dark=True)


def a_land():
    return app(f'''<div class="pl full land">{video("fill", sub=False)}<div class="pl-scrim"></div>
<header class="pl-top">{BACK}{title()}</header>
<div class="pl-center">{transport(play_cls="")}</div>
<footer class="pl-bottom">{timeline("a")}
<div class="pl-row split"><div class="pl-group">{LOCK}{ROTATE}{SPEED}</div><div class="pl-group end">{SUBS}{AUDIO}{ASPECT}{PIP}{MORE}</div></div></footer></div>''', dark=True)


# ---------------------------------------------------------------- B · Dock
def pill(inner, cls=""):
    return f'<div class="{("pl-pill " + cls).strip()}">{inner}</div>'


B_CHAPTER = pill(f'{ic("segment")}<span>4 · The crossing</span>', "tag")


def b_dock(full, paused=False, tip=False):
    right = f'{SUBS_TIP if tip else SUBS}{AUDIO}{SPEED}{MORE}{SEP}{PIP}{FS_EXIT if full else FS}'
    return (f'<div class="pl-row"><div class="pl-group">{pill(MUTE + vol())}</div>{pill(transport(paused=paused), "dock")}'
            f'<div class="pl-group end">{pill(right)}</div></div>')


def b_full():
    return app(f'''<div class="pl full">{video("fill", lift=True)}
<header class="pl-top">{pill(BACK + title(), "titled")}{GROW}{B_CHAPTER}</header>{GROW}
<footer class="pl-bottom">{pill(timeline("b"), "line")}{b_dock(True)}</footer></div>''', dark=True)


def b_win(paused=False, kb=False, tip=False):
    bubble = (55.0, "28:40", "The crossing") if kb else None
    return app(f'''<div class="pl win"><header class="pl-top">{pill(BACK + title(), "titled")}{GROW}{B_CHAPTER}</header>
<div class="pl-fit"><div class="pl-glow" aria-hidden="true">{ART}</div>{video("boxed")}</div>
<footer class="pl-bottom">{pill(timeline("b", paused=paused, focus=kb, bubble=bubble), "line")}{b_dock(False, paused, tip)}</footer></div>''')


def b_pip():
    return app(f'''<div class="pl full pip">{video("fill", sub=False)}
<header class="pl-top">{pill(RESTORE + CLOSE)}</header>
<div class="pl-center">{pill(transport(prevnext=False, side_cls="sm"), "dock")}</div>
<div class="pl-pill tag pl-ptime"><span>24:16 / 52:04</span></div>
<div class="pl-edge" aria-hidden="true"><i></i></div></div>''', dark=True)


def tile(icon, label, value=""):
    small = f"<small>{value}</small>" if value else ""
    return f'<button class="pl-tile">{ic(icon)}<span>{label}</span>{small}</button>'


def b_port():
    tiles = (tile("subtitles", "Subtitles", "English") + tile("music_note", "Audio", "English 5.1") + tile("speed", "Speed", "1×")
             + tile("aspect_ratio", "Aspect", "Fit") + tile("lock", "Lock") + tile("screen_rotation", "Rotate")
             + tile("picture_in_picture_alt", "PiP") + tile("more_horiz", "More"))
    return app(f'''<div class="pl full port"><header class="pl-top">{pill(BACK + title(), "titled")}</header>
<div class="pl-fit"><div class="pl-glow" aria-hidden="true">{ART}</div>{video("boxed")}</div>
<footer class="pl-bottom">{pill(timeline("b"), "line")}{pill(transport(), "dock")}<div class="pl-tiles">{tiles}</div></footer></div>''', dark=True)


def b_land():
    return app(f'''<div class="pl full land">{video("fill", sub=False)}
<header class="pl-top">{pill(BACK + title(), "titled")}</header>{GROW}
<footer class="pl-bottom">{pill(timeline("b"), "line")}
<div class="pl-row"><div class="pl-group">{pill(LOCK + ROTATE)}</div>{pill(transport(), "dock")}<div class="pl-group end">{pill(SUBS + AUDIO + SPEED + MORE)}</div></div></footer></div>''', dark=True)


# ---------------------------------------------------------------- C · Console
def chip(icon, value, label, key=None):
    keys = f' aria-keyshortcuts="{key}"' if key else ""
    return f'<button class="pl-chip" aria-haspopup="menu" aria-label="{label}: {value}"{keys}>{ic(icon)}<span>{value}</span></button>'


C_SUBS = chip("subtitles", "English", "Subtitles", "C")
C_AUDIO = chip("music_note", "English 5.1", "Audio track", "A")
C_SPEED = chip("speed", "1×", "Playback speed")
C_BACK = btn("arrow_back", "Close player", "tonal", key="Escape")


def c_console(full, paused=False, kb=False):
    bubble = (55.0, "28:40", "The crossing") if kb else None
    play, back10, fwd10 = (btn("play_arrow" if paused else "pause", "Play" if paused else "Pause", "play row" + (" is-paused" if paused else ""), key="Space"),
                           btn("replay_10", "Back 10 seconds", key="J"), btn("forward_10", "Forward 10 seconds", key="L"))
    prev, nxt = btn("skip_previous", "Previous episode", key="Shift+P"), btn("skip_next", "Next episode", key="Shift+N")
    left = f'{play}{back10}{fwd10}{SEP}{prev}{nxt}{SEP}{MUTE}' + (vol() if full else "")
    mid = '<p class="pl-now grow">Chapter 4 of 7 · <b>The crossing</b></p>' if full else GROW
    right = f'{C_SUBS}{C_AUDIO}{C_SPEED}{MORE}{SEP}{PIP}{FS_EXIT if full else FS}'
    return (f'<footer class="pl-console">{timeline("c", paused=paused, focus=kb, bubble=bubble)}'
            f'<div class="pl-row split"><div class="pl-group">{left}</div>{mid}<div class="pl-group end">{right}</div></div></footer>')


def c_side(with_next=True):
    rows = ""
    for i, (name, at, _) in enumerate(CHAPTERS, start=1):
        cur = i == 4
        lead = ic("play_arrow") if cur else f"<span>{i}</span>"
        rows += (f'<li><button{" aria-current=\"true\"" if cur else ""}>{lead if not cur else "<span>" + lead + "</span>"}'
                 f'<span class="truncate">{name}</span><span>{at}</span></button></li>')
    nxt = next_card() if with_next else ""
    return (f'<aside class="pl-side" aria-label="Chapters and up next"><div class="pl-tabs" role="tablist">'
            f'<button role="tab" aria-selected="true">Chapters</button><button role="tab" aria-selected="false">Up next</button></div>'
            f'<ol class="pl-chapters">{rows}</ol>{nxt}</aside>')


def c_full():
    return app(f'''<div class="pl full">{video("fill", lift=True)}<div class="pl-scrim"></div>
<header class="pl-top">{C_BACK}{title()}</header>{GROW}{c_console(True)}</div>''', dark=True)


def c_win(paused=False, kb=False):
    return app(f'''<div class="pl win"><div class="pl-main"><header class="pl-top">{C_BACK}{title()}</header>
<div class="pl-fit">{video("boxed")}</div>{c_console(False, paused, kb)}</div>{c_side()}</div>''')


def c_pip():
    play = btn("pause", "Pause", "play row", key="Space")
    return app(f'''<div class="pl pip">{video("", sub=False)}
<div class="pl-strip"><div class="pl-edge" aria-hidden="true"><i></i></div>{play}{btn("replay_10", "Back 10 seconds", "sm")}{btn("forward_10", "Forward 10 seconds", "sm")}
<span class="pl-t"><b>24:16</b> / 52:04</span>{GROW}{RESTORE}{CLOSE}</div></div>''', dark=True)


def c_port():
    play, back10, fwd10 = btn("pause", "Pause", "play row"), btn("replay_10", "Back 10 seconds"), btn("forward_10", "Forward 10 seconds")
    prev, nxt = btn("skip_previous", "Previous episode"), btn("skip_next", "Next episode")
    chips = C_SUBS + C_AUDIO + C_SPEED + chip("aspect_ratio", "Fit", "Aspect ratio") + ROTATE + LOCK + PIP
    return app(f'''<div class="pl port"><header class="pl-top">{C_BACK}{title()}</header>
<div class="pl-fit">{video("boxed")}</div>
<footer class="pl-console">{timeline("c")}<div class="pl-row split"><div class="pl-group">{play}{back10}{fwd10}</div><div class="pl-group end">{prev}{nxt}{MORE}</div></div>
<div class="pl-chips">{chips}</div></footer>{c_side(with_next=False)}</div>''', dark=True)


def c_land():
    play, back10, fwd10 = btn("pause", "Pause", "play row"), btn("replay_10", "Back 10 seconds"), btn("forward_10", "Forward 10 seconds")
    prev, nxt = btn("skip_previous", "Previous episode"), btn("skip_next", "Next episode")
    return app(f'''<div class="pl full land">{video("fill", sub=False)}<div class="pl-scrim"></div>
<header class="pl-top">{C_BACK}{title()}</header>{GROW}
<footer class="pl-console">{timeline("c")}<div class="pl-row split"><div class="pl-group">{play}{back10}{fwd10}{prev}{nxt}</div>
<div class="pl-group end">{C_SUBS}{C_AUDIO}{C_SPEED}{LOCK}{ROTATE}{MORE}</div></div></footer></div>''', dark=True)


# ---------------------------------------------------------------- component specimens
def specimen(opt):
    def wrap_time(html):
        if opt == "b":
            return pill(html, "line")
        if opt == "c":
            return f'<div class="pl-console" style="border-radius:var(--radius-sm)">{html}</div>'
        return html

    tonal = "tonal" if opt == "c" else ""
    buttons = (btn("subtitles", "Subtitles", tonal) + btn("subtitles", "Subtitles, hover", tonal + " is-hover") + btn("subtitles", "Subtitles, focus", tonal + " is-focus")
               + btn("subtitles", "Subtitles, pressed", tonal + " is-pressed") + btn("subtitles", "Subtitles, disabled", tonal + " is-disabled"))
    plays = (btn("play_arrow", "Play", "play row is-paused") + btn("pause", "Pause", "play row") + btn("pause", "Pause, pressed", "play row is-pressed")
             + btn("pause", "Buffering", "play row is-buffering"))
    keys = "".join(f'<kbd class="kbd">{k}</kbd>' for k in ["Space", "K", "J", "L", "←", "→", "↑", "↓", "M", "C", "F", "I", "?", "Esc"])
    menu = ('<div class="menu pl-menu" role="menu" aria-label="Subtitles"><div class="menu-label"><span>Subtitles</span><kbd class="kbd">S</kbd></div>'
            '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>Off</span></span></button>'
            '<button class="menu-item is-hover" role="menuitemradio" aria-checked="true"><span class="radio"></span><span class="text"><span>English</span></span></button>'
            '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>English</span><span>For the deaf and hard of hearing</span></span><span class="tag">SDH</span></button>'
            '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>Deutsch</span></span></button>'
            f'<hr><button class="menu-item" role="menuitem">{ic("tune")}<span class="text"><span>Subtitle style</span></span>{ic("chevron_right")}</button></div>')
    where = {"a": "centre of the picture, controls stay hidden", "b": "pill at the top centre", "c": "label just above the console"}[opt]
    return f'''<div class="app" style="min-height:0;padding:var(--space-5);border-radius:12px">
<div class="px-spec">
<figure><div class="line">{buttons}</div><figcaption>Icon button · default, hover, focus, pressed (shape change), disabled · {TOKENS[opt]["size"]["control"]} px, touch {TOKENS[opt]["size"]["touch"]} px</figcaption></figure>
<figure><div class="line">{plays}</div><figcaption>Play · paused, playing, pressed, buffering. Shape tells paused from playing, not colour alone</figcaption></figure>
<figure><div class="pl" style="height:auto;padding-top:44px">{wrap_time(timeline(opt, focus=True, bubble=(55.0, "28:40", "The crossing")))}</div><figcaption>Timeline · keyboard focus, seek bubble with chapter. ← → 5 s, J L 10 s, Ctrl+← → chapter</figcaption></figure>
<figure><div class="line" style="padding-top:44px">{btn("volume_up", "Mute", tonal + " is-hover", key="M", tip=True)}<span style="width:var(--space-8)"></span>{keys}</div><figcaption>Tooltip carries the shortcut · key caps for the cheat sheet and Settings</figcaption></figure>
<figure><div class="line on-video" data-theme-panel="dark">{bezel("forward_10", "+10 s", static=True)}{bezel("volume_up", "65 %", meter=65, static=True)}{bezel("speed", "1.25×", static=True)}</div><figcaption>Shortcut feedback · {where}; announced to screen readers</figcaption></figure>
<figure><div class="rel">{menu}</div><figcaption>Menu over the player · radio items, shortcut in the header, link to Subtitle style</figcaption></figure>
<figure>{next_card(actions=True)}<figcaption>Up next · last 10 s of an episode. Enter plays now, Esc cancels</figcaption></figure>
<figure class="wide"><div class="px-subs"><p><span>{SUB_LINE}</span></p><p class="outline"><span>{SUB_LINE}</span></p><p class="s"><span>— Is that the coast? — Not yet. 0123456789</span></p></div>
<figcaption>Subtitles · {TOKENS[opt]["font"]["subtitle"]["family"]} · boxed, outline, small. App UI stays on the system font</figcaption></figure>
</div></div>'''


# ---------------------------------------------------------------- page
OPTIONS = {
    "a": {
        "name": "Centre Stage",
        "lead": "Today's player with the timeline freed. PiP and Fullscreen leave the timeline and drop into a row under it, joined by volume and the track buttons. Fullscreen keeps the big centre Play you already know; windowed moves every control under the picture.",
        "mood": "Mood: familiar, quiet, cinematic · Controls: tonal 48 px circles, 80 px Play · Timeline: Material slider, white over video · Subtitles: Atkinson Hyperlegible",
        "trade": "Trade-off: the transport sits in the centre in fullscreen but in the row when windowed, and the margins left and right of the windowed picture stay empty.",
        "full": a_full, "win": a_win, "pip": a_pip, "port": a_port, "land": a_land, "pip_cls": "",
    },
    "b": {
        "name": "Dock",
        "lead": "One floating dock in every mode. All controls live in opaque pills, so the picture needs no dark gradient and the layout is identical windowed and fullscreen. Windowed mode fills the margin with a soft glow taken from the frame.",
        "mood": "Mood: expressive, tactile, calm · Controls: pills, 64 px Play that changes shape · Timeline: wavy while playing, flat when paused · Subtitles: Nunito",
        "trade": "Trade-off: the pills cover part of the lower picture in fullscreen, and the centre of the picture no longer has a Play button to click (click anywhere still toggles).",
        "full": b_full, "win": b_win, "pip": b_pip, "port": b_port, "land": b_land, "pip_cls": "",
    },
    "c": {
        "name": "Console",
        "lead": "A desk player. An opaque console shows the current subtitle, audio and speed as labelled values instead of bare icons, and windowed mode turns the empty margin into a Chapters and Up next panel.",
        "mood": "Mood: informative, precise, desk-first · Controls: 40 px squircles on desktop (48 px on touch), Play at the left · Timeline: segmented by chapter · Subtitles: Barlow Semi Condensed",
        "trade": "Trade-off: the densest and the furthest from today's UI; the console covers the bottom of the picture while visible.",
        "full": c_full, "win": c_win, "pip": c_pip, "port": c_port, "land": c_land, "pip_cls": "tall",
    },
}


def pip_frame(o, zoom=""):
    kind = "pip" + (" " + o["pip_cls"] if o["pip_cls"] else "")
    return f'<div class="v-frame pf pf-{kind} {zoom}">{o["pip"]()}</div>'


def option_section(key):
    o = OPTIONS[key]
    win_light = o["win"](tip=True) if key != "c" else o["win"]()
    return f'''<section class="v-option opt-{key}" data-tokens="opt-{key}" id="opt-{key}">
<header><h2>{key.upper()} · {o["name"]}</h2><p>{o["lead"]}</p><p>{o["trade"]}</p><p class="v-label">{o["mood"]}</p></header>
<div class="v-section"><h2>Desktop · windowed</h2><p>Controls never cover the picture. Left: playing, light theme. Right: paused, dark theme, keyboard seeking with the chapter bubble.</p>
<div class="px-row">{cell("Windowed · light · playing", frame("desk", win_light, "light", "z50"))}{cell("Windowed · dark · paused · seeking by keyboard", frame("desk", o["win"](paused=True, kb=True), "dark", "z50"))}</div></div>
<div class="v-section"><h2>Desktop · fullscreen and PiP</h2><p>Over the picture the player always uses the dark scheme, in both app themes.</p>
<div class="px-row">{cell("Fullscreen · controls visible", frame("desk", o["full"](), None, "z62"))}{cell("PiP mini window · 480 × 270", pip_frame(o))}</div></div>
<div class="v-section"><h2>Android phone</h2>
<div class="px-row">{cell("Portrait", frame("port", o["port"](), None, "z66"))}{cell("Landscape", frame("land", o["land"](), None, "z66"))}</div></div>
<div class="v-section"><h2>Components</h2>{specimen(key)}</div>
<div class="v-section"><h2>Type</h2><p>System font for the app; the subtitle style is the only bundled typeface.</p><div class="v-card" data-render="type"></div></div>
</section>'''


def compare():
    cols = ""
    for key, o in OPTIONS.items():
        cols += (f'<div class="px-cell opt-{key}" data-tokens="opt-{key}"><p class="v-label">{key.upper()} · {o["name"]} · fullscreen</p>{frame("desk", o["full"](), None, "z33")}'
                 f'<p class="v-label" style="margin-top:16px">{key.upper()} · windowed, dark</p>{frame("desk", o["win"](), "dark", "z33")}'
                 f'<p class="v-label" style="margin-top:16px">{key.upper()} · windowed, light</p>{frame("desk", o["win"](), "light", "z33")}</div>')
    return f'<section class="v-section" id="compare"><h2>Side by side</h2><p>Same frame, same moment, three layouts.</p><div class="px-row">{cols}</div></section>'


INTRO = '''<section class="v-section"><h2>Round 1 · three layouts for the player</h2>
<p>Palette, shapes and the system UI font are fixed by the approved Marquee direction, so these three differ in layout, control shape, timeline style and subtitle typeface. In all three the timeline is full width with only the two timers beside it, every other control sits below it, and every control shows its shortcut.</p>
<div class="px-scroll"><table class="px-table"><thead><tr><th></th><th>A · Centre Stage</th><th>B · Dock</th><th>C · Console</th></tr></thead><tbody>
<tr><th>Fullscreen</th><td>Big Play stays in the centre of the picture; other controls in a row under the timeline</td><td>Clean picture; three floating pills under the timeline</td><td>Opaque console across the bottom, Play at the left, labelled values</td></tr>
<tr><th>Windowed</th><td>Picture on top, timeline and one control row under it on the app surface</td><td>Same dock as fullscreen, on a glow taken from the frame</td><td>Picture and console left, Chapters / Up next panel right</td></tr>
<tr><th>PiP</th><td>Title, thin timeline, three buttons over the picture</td><td>Clean picture with a progress edge; small dock on hover or focus</td><td>Control strip under the picture, nothing over it</td></tr>
<tr><th>Phone portrait</th><td>As today: timeline, transport, scrolling quick actions</td><td>Dock plus eight labelled tiles (no scrolling row)</td><td>Console, value chips and the chapter list</td></tr>
<tr><th>Distance from today</th><td>Smallest</td><td>Medium</td><td>Largest</td></tr>
</tbody></table></div></section>'''

FOUNDATIONS = '''<section class="v-section opt-a" data-tokens="opt-a" id="foundations"><h2>Shared foundations</h2>
<p>Identical in all three directions: the Marquee palette, plus three roles for chrome over video (video, on-video, video-scrim-floor). The scrim floor is the lightest result of the scrim over a white frame where text or icons may sit.</p>
<div data-render="roles"></div><h2 style="margin-top:32px">Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div></section>'''


def page():
    tokens = "\n".join(f'<!-- tokens: ../option-{k}.tokens.json scope=.opt-{k} id=opt-{k} -->' for k in OPTIONS)
    body = INTRO + "".join(option_section(k) for k in OPTIONS) + compare() + FOUNDATIONS
    return f'''<title>EmbyEx Player Directions</title>
{tokens}
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: options.css --></style>
<script>window.DESIGN = {{ app: "EmbyEx Player · Round 1", mode: "options", themes: ["light", "dark"] }};</script>
<div class="v-options">
{body}
</div>
<script><!-- include: viewer.js --></script>
'''


def main():
    for key, data in TOKENS.items():
        (OUT / f"option-{key}.tokens.json").write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
    (OUT / "src" / "index.html").write_text(page())
    print("wrote tokens and", OUT / "src" / "index.html")


if __name__ == "__main__":
    main()
