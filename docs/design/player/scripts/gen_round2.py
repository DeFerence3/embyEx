#!/usr/bin/env python3
"""Generates round 2 of the player directions: the remix D · Dockside (B's dock + C's heading and side panel).
Writes options/round-2/option-d.tokens.json and options/round-2/src/index.html, reusing the pieces of round 1.
Run from anywhere:  python3 docs/design/player/scripts/gen_round2.py"""
import copy
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_options as g  # noqa: E402
from gen_options import (ART, AUDIO, BACK, CHAPTERS, FS, FS_EXIT, GROW, LOCK, MORE, MUTE, PIP, ROTATE, SEP, SPEED, SUB_LINE, SUBS,  # noqa: E402
                         SUBS_TIP, app, bezel, btn, cell, frame, ic, next_card, pill, tile, timeline, title, transport, video, vol)

OUT = g.ROOT / "options" / "round-2"

# ---------------------------------------------------------------- tokens: B's, plus the two other subtitle candidates for the specimen
TOKENS = copy.deepcopy(g.TOKENS["b"])
TOKENS["meta"]["name"] = "Dockside"
TOKENS["meta"]["rationale"] = ("B's floating dock, wavy timeline and glow, with C's transparent heading and Chapters / Up next panel. "
                               "The dock is regrouped and the panel restyled so the two read as one design.")
TOKENS["font"]["subtitle-atkinson"] = g.TOKENS["a"]["font"]["subtitle"]
TOKENS["font"]["subtitle-barlow"] = g.TOKENS["c"]["font"]["subtitle"]

# ---------------------------------------------------------------- pieces
CHAPNOW = f'<span class="pl-chapnow">{ic("segment")}Chapter 4 · The crossing</span>'
EPISODES = [("S2 · E6 · 48 min", "Dead Reckoning", "Next"), ("S2 · E7 · 51 min", "Slack Water", ""), ("S2 · E8 · 58 min", "Home Light", "Season finale")]


def panel_btn(is_open):
    icon, label = ("right_panel_close", "Hide chapters and up next") if is_open else ("right_panel_open", "Show chapters and up next")
    return f'<button class="pl-btn" aria-label="{label}" aria-pressed="{str(is_open).lower()}" aria-keyshortcuts="P">{ic(icon)}</button>'


def chapter_rows(first=1):
    """first > 1 shows the list scrolled, as it is when the playing chapter is kept in view."""
    rows = ""
    for i, (name, at, _) in enumerate(CHAPTERS, start=1):
        if i < first:
            continue
        cur = i == 4
        num = ic("play_arrow") if cur else str(i)
        label = f"Chapter {i}, {name}, starts at {at}" + (", playing" if cur else "")
        rows += (f'<li><button class="pl-chap-row" aria-label="{label}"{" aria-current=\"true\"" if cur else ""}>'
                 f'<span class="num">{num}</span><span class="truncate">{name}</span><span class="at">{at}</span></button></li>')
    return rows


def episode_rows():
    rows = ""
    for meta, name, tag in EPISODES:
        badge = f'<span class="tag">{tag}</span>' if tag else ""
        rows += (f'<li><button class="pl-ep{" is-next" if tag == "Next" else ""}"><span class="thumb">{ART}</span>'
                 f'<span class="meta"><small>{meta}</small><strong class="truncate">{name}</strong>{badge}</span></button></li>')
    return rows


def side(tab="chapters", compact=False):
    chapters = tab == "chapters"
    tabs = (f'<div class="pl-tabs" role="tablist" aria-label="Chapters and up next">'
            f'<button role="tab" aria-selected="{str(chapters).lower()}">{ic("segment")}Chapters</button>'
            f'<button role="tab" aria-selected="{str(not chapters).lower()}">{ic("playlist_play")}Up next</button></div>')
    if chapters:
        body = f'<ol class="pl-list" aria-label="Chapters">{chapter_rows(first=3 if compact else 1)}</ol>' + ("" if compact else next_card())
    else:
        body = (f'<ol class="pl-list" aria-label="Up next">{episode_rows()}</ol>'
                '<label class="switch pl-auto"><input type="checkbox" checked><span>Play next automatically</span></label>')
    return f'<aside class="pl-side" aria-label="Chapters and up next">{tabs}{body}</aside>'


SUBS_MENU = ('<div class="menu pl-menu{cls}" role="menu" aria-label="Subtitles"><div class="menu-label"><span>Subtitles</span><kbd class="kbd">S</kbd></div>'
             '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>Off</span></span></button>'
             '<button class="menu-item is-hover" role="menuitemradio" aria-checked="true"><span class="radio"></span><span class="text"><span>English</span></span></button>'
             '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>English</span><span>For the deaf and hard of hearing</span></span><span class="tag">SDH</span></button>'
             '<button class="menu-item" role="menuitemradio" aria-checked="false"><span class="radio"></span><span class="text"><span>Deutsch</span></span></button>'
             '<hr><button class="menu-item" role="menuitem">' + ic("tune") + '<span class="text"><span>Subtitle style</span></span>' + ic("chevron_right") + '</button></div>')


def dock(full, paused=False, tip=False, panel=True, menu=False):
    subs = btn("subtitles", "Subtitles", "is-hover", key="C") if menu else (SUBS_TIP if tip else SUBS)
    left = f'{MUTE}{vol()}{SEP}{AUDIO}{subs}' + (SUBS_MENU.format(cls=" up") if menu else "")
    right = f'{SPEED}{MORE}{SEP}{panel_btn(panel)}{PIP}{FS_EXIT if full else FS}'
    return (f'<div class="pl-row"><div class="pl-group">{pill(left)}</div>{pill(transport(paused=paused), "dock")}'
            f'<div class="pl-group end">{pill(right)}</div></div>')


def head(panel=True):
    return f'<header class="pl-top">{BACK}{title()}{"" if panel else CHAPNOW}</header>'


# ---------------------------------------------------------------- frames
def d_win(paused=False, kb=False, tip=False, tab="chapters", panel=True, menu=False):
    bubble = (55.0, "28:40", "The crossing") if kb else None
    main = (f'<div class="pl-main">{head(panel)}<div class="pl-fit"><div class="pl-glow" aria-hidden="true">{ART}</div>{video("boxed")}</div>'
            f'<footer class="pl-bottom">{pill(timeline("b", paused=paused, focus=kb, bubble=bubble), "line")}{dock(False, paused, tip, panel, menu)}</footer></div>')
    return app(f'<div class="pl win{"" if panel else " solo"}">{main}{side(tab) if panel else ""}</div>')


def d_full(panel=False, bez=False):
    main = (f'<div class="pl-main">{head(panel)}{GROW}'
            f'<footer class="pl-bottom">{pill(timeline("b"), "line")}{dock(True, panel=panel)}</footer></div>')
    feedback = bezel("volume_up", "65 %", meter=65) if bez else ""
    return app(f'<div class="pl full{" paneled" if panel else ""}">{video("fill", lift=True)}<div class="pl-scrim"></div>{main}{side() if panel else ""}{feedback}</div>', dark=True)


def d_port():
    tiles = (tile("subtitles", "Subtitles", "English") + tile("music_note", "Audio", "English 5.1") + tile("speed", "Speed", "1×")
             + tile("aspect_ratio", "Aspect", "Fit") + tile("lock", "Lock") + tile("screen_rotation", "Rotate")
             + tile("picture_in_picture_alt", "PiP") + tile("more_horiz", "More"))
    main = (f'<div class="pl-main">{head()}<div class="pl-fit"><div class="pl-glow" aria-hidden="true">{ART}</div>{video("boxed")}</div>{side(compact=True)}'
            f'<footer class="pl-bottom">{pill(timeline("b"), "line")}{pill(transport(), "dock")}<div class="pl-tiles">{tiles}</div></footer></div>')
    return app(f'<div class="pl full port">{main}</div>', dark=True)


def d_land():
    main = (f'<div class="pl-main">{head()}{GROW}<footer class="pl-bottom">{pill(timeline("b"), "line")}'
            f'<div class="pl-row"><div class="pl-group">{pill(LOCK + ROTATE + SPEED)}</div>{pill(transport(), "dock")}'
            f'<div class="pl-group end">{pill(SUBS + AUDIO + panel_btn(False) + MORE)}</div></div></footer></div>')
    return app(f'<div class="pl full land">{video("fill", sub=False)}<div class="pl-scrim"></div>{main}</div>', dark=True)


# ---------------------------------------------------------------- components
def specimen():
    buttons = (btn("subtitles", "Subtitles") + btn("subtitles", "Subtitles, hover", "is-hover") + btn("subtitles", "Subtitles, focus", "is-focus")
               + btn("subtitles", "Subtitles, pressed", "is-pressed") + btn("subtitles", "Subtitles, disabled", "is-disabled"))
    plays = (btn("play_arrow", "Play", "play row is-paused") + btn("pause", "Pause", "play row") + btn("pause", "Pause, pressed", "play row is-pressed")
             + btn("pause", "Buffering", "play row is-buffering"))
    keys = "".join(f'<kbd class="kbd">{k}</kbd>' for k in ["Space", "K", "J", "L", "←", "→", "↑", "↓", "M", "C", "S", "A", "P", "F", "I", "?", "Esc"])
    rows = (f'<li><button class="pl-chap-row"><span class="num">3</span><span class="truncate">Harbour</span><span class="at">4:40</span></button></li>'
            f'<li><button class="pl-chap-row is-hover"><span class="num">5</span><span class="truncate">Storm front</span><span class="at">33:30</span></button></li>'
            f'<li><button class="pl-chap-row is-focus"><span class="num">6</span><span class="truncate">Landfall</span><span class="at">45:10</span></button></li>'
            f'<li><button class="pl-chap-row" aria-current="true"><span class="num">{ic("play_arrow")}</span><span class="truncate">The crossing</span><span class="at">19:05</span></button></li>')
    tabs = (f'<div class="pl-tabs" role="tablist"><button role="tab" aria-selected="true">{ic("segment")}Chapters</button>'
            f'<button role="tab" aria-selected="false">{ic("playlist_play")}Up next</button></div>')
    tiles = tile("subtitles", "Subtitles", "English") + tile("music_note", "Audio", "English 5.1") + tile("speed", "Speed", "1×") + tile("lock", "Lock")
    faces = "".join(
        f'<p style="font-family:var(--font-{role})"><span>{SUB_LINE}</span><small>{name}</small></p>'
        for role, name in [("subtitle", "Nunito · shown in the frames above"), ("subtitle-atkinson", "Atkinson Hyperlegible"), ("subtitle-barlow", "Barlow Semi Condensed")])
    return f'''<div class="app" style="min-height:0;padding:var(--space-5);border-radius:12px">
<div class="px-spec">
<figure><div class="line">{buttons}</div><figcaption>Icon button · default, hover, focus, pressed (shape change), disabled · 48 px. Inside a pill the container is the pill</figcaption></figure>
<figure><div class="line">{plays}</div><figcaption>Play · paused (round), playing (squircle), pressed, buffering</figcaption></figure>
<figure><div class="pl" style="height:auto;padding-top:44px">{pill(timeline("b", focus=True, bubble=(55.0, "28:40", "The crossing")), "line")}</div><figcaption>Timeline · wavy while playing, keyboard focus, seek bubble with chapter</figcaption></figure>
<figure><div class="line" style="padding-top:44px">{btn("volume_up", "Mute", "is-hover", key="M", tip=True)}<span style="width:var(--space-8)"></span>{keys}</div><figcaption>Tooltip carries the shortcut · key caps for the cheat sheet and Settings</figcaption></figure>
<figure><div class="line on-video" data-theme-panel="dark">{bezel("forward_10", "+10 s", static=True)}{bezel("volume_up", "65 %", meter=65, static=True)}{bezel("speed", "1.25×", static=True)}</div><figcaption>Shortcut feedback · pill at the top centre, also announced to screen readers</figcaption></figure>
<figure><div class="rel">{SUBS_MENU.format(cls="")}</div><figcaption>Menu · rounded like the pills, radio rows, shortcut in the header, link to Subtitle style</figcaption></figure>
<figure><div class="pl-side" style="width:auto">{tabs}<ol class="pl-list" style="margin:0;gap:var(--space-1)">{rows}</ol></div><figcaption>Panel · pill tabs; chapter row default, hover, focus, playing. One Tab stop; ↑ ↓ move, Enter jumps</figcaption></figure>
<figure>{next_card(actions=True)}<figcaption>Up next · last 10 s of an episode. Enter plays now, Esc cancels</figcaption></figure>
<figure><div class="pl-tiles" data-theme-panel="dark" style="padding:var(--space-3);border-radius:var(--radius-md);background:var(--color-video)">{tiles}</div><figcaption>Phone tiles · label and current value, no scrolling row</figcaption></figure>
<figure class="wide"><div class="px-subs">{faces}</div><figcaption>Subtitle typeface candidates on the same line · the default is your pick; all three can be offered on the Subtitle style page</figcaption></figure>
</div></div>'''


# ---------------------------------------------------------------- page
INTRO = '''<section class="v-section"><h2>Round 2 · D · Dockside</h2>
<p>B's dock is the base. C contributes the transparent heading with a leading back button and the Chapters / Up next panel. Dropping those two in unchanged would leave three pills fighting a panel for width and two chapter indicators, so the pieces around them were redesigned.</p>
<div class="px-scroll"><table class="px-table"><thead><tr><th>Element</th><th>Comes from</th><th>What changed to make it fit</th></tr></thead><tbody>
<tr><th>Heading</th><td>C</td><td>Back button and title sit directly on the surface (windowed) or on a short top gradient (fullscreen). B's title pill is gone. The back button takes the dock's round tonal shape.</td></tr>
<tr><th>Side panel</th><td>C</td><td>Restyled with pill tabs, pill-shaped rows and the same corner radius as the picture. It can be closed with a dock button or P. In fullscreen it opens beside the picture, which resizes so subtitles are never covered. On phone portrait it fills the space between picture and dock and keeps the playing chapter in view.</td></tr>
<tr><th>Dock</th><td>B</td><td>Regrouped so both side pills are the same width and the transport stays centred beside the panel: sound and language on the left (volume, audio, subtitles), playback and window on the right (speed, more, panel, PiP, fullscreen).</td></tr>
<tr><th>Chapter indicator</th><td>B</td><td>B's top-right chapter pill is removed. With the panel open the list shows the chapter; with it closed a quiet label appears at the end of the heading.</td></tr>
<tr><th>Picture</th><td>B</td><td>Windowed: fills the column beside the panel; with the panel closed it grows and centres. The glow stays in both.</td></tr>
<tr><th>Up next</th><td>C</td><td>A card pinned under the chapters; the Up next tab holds the queue and a switch for playing the next episode automatically.</td></tr>
<tr><th>Menus</th><td>new</td><td>Rounded to match the pills and opening upwards from the dock.</td></tr>
</tbody></table></div></section>'''

FOUNDATIONS = '''<div class="v-section" id="foundations"><h2>Foundations</h2>
<p>The Marquee palette, unchanged, plus three roles for chrome over video.</p>
<div data-render="roles"></div><h2 style="margin-top:32px">Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div></div>'''


def page():
    pip = f'<div class="v-frame pf pf-pip">{g.b_pip()}</div>'
    section = f'''<section class="v-option opt-d" data-tokens="opt-d" id="opt-d">
<header><h2>D · Dockside</h2><p>One dock in every mode, a heading that stays out of the way, and a panel that turns the windowed margin into chapters and what plays next.</p>
<p class="v-label">Mood: expressive, tactile, calm · Controls: pills, 64 px Play that changes shape · Timeline: wavy while playing · Panel: 304 px, closable · Subtitles: Nunito (your pick below)</p></header>
<div class="v-section"><h2>Desktop · windowed, panel open</h2><p>Nothing covers the picture. Left: playing, light theme, Chapters. Right: paused, dark theme, seeking by keyboard, Up next.</p>
<div class="px-row">{cell("Windowed · light · playing · Chapters", frame("desk", d_win(tip=True), "light", "z50"))}{cell("Windowed · dark · paused · seeking by keyboard · Up next", frame("desk", d_win(paused=True, kb=True, tab="upnext"), "dark", "z50"))}</div></div>
<div class="v-section"><h2>Desktop · windowed, panel closed</h2><p>P or the panel button closes the panel. The picture grows, the chapter moves into the heading, and the dock keeps its place. Shown with the Subtitles menu open.</p>
<div class="px-row">{cell("Windowed · light · panel closed · Subtitles menu", frame("desk", d_win(panel=False, menu=True), "light", "z50"))}{cell("Windowed · dark · panel closed", frame("desk", d_win(panel=False), "dark", "z50"))}</div></div>
<div class="v-section"><h2>Desktop · fullscreen</h2><p>Over the picture the player always uses the dark scheme. The only gradient is the short one behind the heading. Right: with the panel open the picture resizes into the free area; volume feedback shown after pressing ↑.</p>
<div class="px-row">{cell("Fullscreen · controls visible", frame("desk", d_full(), None, "z50"))}{cell("Fullscreen · panel open · volume feedback", frame("desk", d_full(panel=True, bez=True), None, "z50"))}</div></div>
<div class="v-section"><h2>PiP and Android phone</h2>
<div class="px-row">{cell("PiP mini window · 480 × 270", pip)}{cell("Phone portrait", frame("port", d_port(), None, "z66"))}{cell("Phone landscape", frame("land", d_land(), None, "z66"))}</div></div>
<div class="v-section"><h2>Components</h2>{specimen()}</div>
<div class="v-section"><h2>Type</h2><p>System font for the app; subtitles are the only bundled typeface.</p><div class="v-card" data-render="type"></div></div>
{FOUNDATIONS}
</section>'''
    return f'''<title>EmbyEx Player Dockside</title>
<!-- tokens: ../option-d.tokens.json scope=.opt-d id=opt-d -->
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: options.css --></style>
<script>window.DESIGN = {{ app: "EmbyEx Player · Round 2", mode: "options", themes: ["light", "dark"] }};</script>
<div class="v-options">
{INTRO}{section}
</div>
<script><!-- include: viewer.js --></script>
'''


def main():
    (OUT / "option-d.tokens.json").write_text(json.dumps(TOKENS, indent=1, ensure_ascii=False) + "\n")
    (OUT / "src" / "index.html").write_text(page())
    print("wrote", OUT / "option-d.tokens.json", "and", OUT / "src" / "index.html")


if __name__ == "__main__":
    main()
