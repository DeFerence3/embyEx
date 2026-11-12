#!/usr/bin/env python3
"""Generates round 1 of the Home directions: three token files and options/round-1/src/index.html.
Run:  python3 docs/design/home/scripts/gen_options.py
Every frame is assembled from the same pieces and the same sample library so the directions stay comparable."""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # docs/design/home
BASE = json.loads((ROOT.parent / "tokens.json").read_text())  # Marquee tokens (brand, fixed)
OUT = ROOT / "options" / "round-1"

# ---------------------------------------------------------------- sample library (invented titles)
HERO = dict(series="Northbound", code="S2 · E5", title="The Long Way Round", left="23 min left", p=56, art="sea",
            aria="Northbound, season 2 episode 5, The Long Way Round, 23 minutes left")
CONTINUE = [
    dict(k="Northbound · S2 · E5", t="The Long Way Round", m="23 min left", p=56, art="sea"),
    dict(k="Movie · 2023", t="Salt and Signal", m="1 h 12 min left", p=31, art="dawn"),
    dict(k="Copper Valley · S1 · E4", t="Ash on the Wind", m="Next up · 48 min", p=None, art="hills"),
    dict(k="The Night Desk · S3 · E1", t="Deadline", m="9 min left", p=82, art="city"),
    dict(k="Movie · 2021", t="Paper Lanterns", m="38 min left", p=64, art="ember"),
]
LIBS = [("Movies", "movie", "412 titles", "dawn"), ("TV Shows", "tv", "38 shows", "night"),
        ("Kids", "child_care", "64 titles", "grove"), ("Concerts", "music_note", "12 titles", "ember")]
NEW_MOVIES = [("Low Tide", "2024", "sea", True), ("The Glass Orchard", "2024", "grove", False), ("Kestrel", "2023", "frost", False),
              ("Halfway to Morning", "2024", "dawn", False), ("Ember Season", "2022", "ember", True), ("A Map of Small Hours", "2024", "night", False)]
NEW_SHOWS = [("Field Notes", "3 new episodes", "hills"), ("Copper Valley", "Season 2 added", "frost"), ("The Night Desk", "1 new episode", "city"),
             ("Harbour Lights", "New series", "sea"), ("Second Shift", "2 new episodes", "ember")]
FAVS = [("Kestrel", "2023", "frost"), ("Northbound", "Series", "sea"), ("The Quiet Harbour", "2019", "night"), ("Paper Lanterns", "2021", "ember")]
DOWNLOADED = [dict(k="Northbound · S2 · E6", t="Low Water", m="On this device · 1.4 GB", p=None, art="sea", dl=True),
              dict(k="Movie · 2024", t="Low Tide", m="On this device · 4.2 GB", p=None, art="frost", dl=True)]


# ---------------------------------------------------------------- tokens
def make_tokens(name, rationale, sizes):
    t = copy.deepcopy(BASE)
    t["meta"]["name"] = name
    t["meta"]["rationale"] = rationale
    for theme in ("light", "dark"):
        # Text over artwork sits on the scrim; artwork stand-ins use these extra hues and look the same in both themes.
        t["color"][theme].update({
            "on-scrim": "{neutral.50}", "on-scrim-variant": "{neutral.300}", "on-scrim-track": "{neutral.700}",
            "art-4": "{blue.800}", "art-5": "{red.800}", "art-6": "{amber.700}",
        })
    t["contrast"] += [
        {"fg": "on-scrim-variant", "bg": ["scrim"], "use": "text"},
        {"fg": "on-scrim", "bg": ["on-scrim-track"], "use": "text"},
        {"fg": "primary", "bg": ["surface-container-highest"], "use": "ui"},
        {"fg": "on-surface", "bg": ["surface-container-highest", "surface-container"], "use": "text"},
        {"fg": "on-tertiary-container", "bg": ["tertiary-container"], "use": "text"},
        {"fg": "primary", "bg": ["surface-container-low"], "use": "ui"},
        {"fg": "on-surface-variant", "bg": ["background"], "use": "text"},
    ]
    t["size"].update(sizes)
    return t


OPTIONS = {
    "a": dict(
        name="Shelf",
        rationale="Today's Home, tidied: a Resume hero card, then the same shelves with clear headings, See all and a ⋮ on every card. Nothing to relearn.",
        lead="Today's Home, tidied. The decorative banner becomes a Resume hero card with the text on a dark scrim; below it the same shelves you know, each with a count, See all and a ⋮ on every card. Libraries keep their asymmetric cards.",
        trade="Trade-off: the most familiar, but most sections are still horizontal shelves, so phone users scroll sideways a lot (See all is the escape hatch).",
        mood="Mood: familiar, tidy, calm · Hero: art card with scrim, 56 px Resume · Sections: shelves · Libraries: art cards",
        sizes={"hero": 300},
    ),
    "b": dict(
        name="Spotlight",
        rationale="Cinematic and bold, like the Details screen: full-bleed art fading into the page, a large title and a 64 px Resume; libraries become quick buttons.",
        lead="Cinematic, matching the Details screen: full-bleed artwork fades into the page, the title sits below it in display size, and Resume is a full-width 64 px button. Libraries turn into a grid of quick buttons (no sideways scrolling), shelves get larger cards with the expressive corner shape that morphs on hover and focus.",
        trade="Trade-off: the hero takes most of the first phone screen, so Continue watching starts lower; boldest of the three.",
        mood="Mood: cinematic, expressive, confident · Hero: full-bleed, display title, 64 px Resume · Sections: big shelves · Libraries: button grid",
        sizes={"hero": 300},
    ),
    "c": dict(
        name="Index",
        rationale="Accessibility first: no text over artwork, Continue watching as a vertical list, libraries as big tiles. Least sideways scrolling, best at 200 % text.",
        lead="Accessibility first. No text ever sits on artwork: the hero is a tonal panel with a thumbnail, and Continue watching is a vertical list with full titles and progress. Libraries are large tiles in a grid. Only the 'New in' shelves scroll sideways.",
        trade="Trade-off: the calmest and most robust, but the least cinematic; artwork is smaller.",
        mood="Mood: clear, calm, dependable · Hero: tonal panel, 56 px Resume · Sections: lists + tiles · Libraries: 2-column tiles",
        sizes={"hero": 0},
    ),
}


# ---------------------------------------------------------------- shared pieces
def ic(name, cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<i data-icon="{name}"{c}></i>'


def art(scene):
    return f'<div class="art {scene}" aria-hidden="true"><i></i><b></b><b></b></div>'


def src_chip(kind="ok"):
    data = {
        "ok": ("", "cloud_done", "Harbour", "· Connected", "Source: Harbour server, connected. Show details"),
        "warn": ("warn", "sync", "Harbour", "· Reconnecting…", "Source: Harbour server, reconnecting. Show details"),
        "off": ("off", "cloud_off", "Offline", "· Downloads only", "Offline, showing downloads only. Show details"),
        "local": ("local", "smartphone", "This device", "", "Source: files on this device. Show details"),
    }[kind]
    cls, icon, name, st, aria = data
    return f'<button class="src {cls}" aria-label="{aria}">{ic(icon)}<span class="truncate">{name} <span class="st">{st}</span></span></button>'


def topbar(chip="ok"):
    return (f'<header class="hm-top"><div class="hm-brand"><h1>EmbyEx</h1>{src_chip(chip)}</div>'
            f'<button class="icon-btn" aria-label="Refresh Home" aria-keyshortcuts="Control+R" title="Refresh (Ctrl+R)">{ic("refresh")}</button></header>')


def head(title, count=None, see_all=True, first=True, scroll=True):
    cnt = f'<span class="count">{count}</span>' if count else ""
    see = (f'<a class="btn text" href="#" aria-label="See all {count or ""} in {title}">See all{ic("chevron_right")}</a>' if see_all else "")
    scr = (f'<span class="scrollers"><button class="icon-btn" aria-label="Scroll {title} back"{" disabled" if first else ""}>{ic("chevron_left")}</button>'
           f'<button class="icon-btn" aria-label="Scroll {title} forward">{ic("chevron_right")}</button></span>') if scroll else ""
    return f'<div class="hm-head"><h2>{title}{cnt}</h2>{see}{scr}</div>'


def wide_card(c, state=""):
    prog = f'<span class="bar" style="--p:{c["p"]}%"></span>' if c.get("p") else ""
    badge = ""
    if c.get("p") is None and "Next up" in c["m"]:
        badge = f'<span class="badge-art left new">{ic("skip_next")}Next up</span>'
    if c.get("dl"):
        badge = f'<span class="badge-art">{ic("download_done")}<span class="v-sr">Downloaded</span></span>'
    aria = f'{c["k"].replace(" · ", ", ")}, {c["t"]}, {c["m"]}'
    return (f'<li class="mc wide"><a class="mc-hit {state}" href="#" aria-label="{aria}"><div class="mc-art">{art(c["art"])}{badge}{prog}</div>'
            f'<div class="mc-text"><span class="mc-k truncate">{c["k"]}</span><h3 class="mc-t truncate">{c["t"]}</h3><span class="mc-m truncate">{c["m"]}</span></div></a>'
            f'<button class="icon-btn mc-more" aria-label="More options for {c["t"]}">{ic("more_vert")}</button></li>')


def poster(t, sub, scene, badge=None, state=""):
    b = ""
    if badge == "new":
        b = '<span class="badge-art new">New</span>'
    elif badge == "watched":
        b = f'<span class="badge-art">{ic("check")}<span class="v-sr">Watched</span></span>'
    elif badge == "dl":
        b = f'<span class="badge-art">{ic("download_done")}<span class="v-sr">Downloaded</span></span>'
    return (f'<li class="mc poster"><a class="mc-hit {state}" href="#" aria-label="{t}, {sub}"><div class="mc-art">{art(scene)}{b}</div>'
            f'<div class="mc-text"><h3 class="mc-t clamp-2">{t}</h3><span class="mc-m truncate">{sub}</span></div></a>'
            f'<button class="icon-btn mc-more" aria-label="More options for {t}">{ic("more_vert")}</button></li>')


def lib_card(name, icon, count, scene, state=""):
    return (f'<li class="lib"><a class="lib-hit {state}" href="#" aria-label="{name}, {count}"><div class="mc-art">{art(scene)}</div>'
            f'<span class="name"><span class="truncate">{name}</span><span class="muted">{count.split()[0]}</span></span></a></li>')


def rail(items, label):
    return f'<ul class="hm-rail" aria-label="{label}">{"".join(items)}</ul>'


def section(title, count, body, first=True):
    sid = title.lower().replace(" ", "-")
    return f'<section class="hm-sec" aria-labelledby="h-{sid}">{head(title, count, first=first).replace("<h2>", f"<h2 id=\"h-{sid}\">", 1)}{body}</section>'


def nav(desk=False):
    tabs = [("home", "Home", True), ("video_library", "Libraries", False), ("search", "Search", False)]
    t = "".join(f'<button class="hm-tab"{" aria-current=\"page\"" if cur else ""}>{ic(i)}<span>{l}</span></button>' for i, l, cur in tabs)
    return (f'<nav class="hm-nav" aria-label="Main"><div class="hm-pill">{t}</div>'
            f'<button class="hm-acct" aria-label="Account and settings, Maya" aria-haspopup="menu"><span class="avatar">MA</span></button></nav>')


EDIT = (f'<div class="hm-edit"><button class="btn outline">{ic("tune")}Edit Home</button>'
        '<p>Choose the hero, card size and which sections show.</p></div>')


def common_sections(desk, posters_cls=""):
    n = 6 if desk else 5
    new_movies = section("New in Movies", "24", rail([poster(t, y, s, "new" if b else None) for t, y, s, b in NEW_MOVIES[:n + 1]], "New in Movies"))
    new_shows = section("New in TV Shows", "11", rail([poster(t, sub, s) for t, sub, s in NEW_SHOWS], "New in TV Shows"))
    favs = section("Favorites", "9", rail([poster(t, sub, s, "watched" if i == 0 else None) for i, (t, sub, s) in enumerate(FAVS)], "Favorites"))
    dls = section("Downloaded", "2", rail([wide_card(c) for c in DOWNLOADED], "Downloaded"))
    return new_movies + new_shows + favs + dls


# ---------------------------------------------------------------- A · Shelf
def a_screen(desk=False):
    hero = (f'<section class="ha-hero" aria-label="Pick up where you left off">{art(HERO["art"])}<div class="in">'
            f'<span class="k">Continue watching · {HERO["series"]} · {HERO["code"]}</span>'
            f'<h2><a href="#" aria-label="{HERO["title"]}, open details">{HERO["title"]}</a></h2>'
            f'<div class="meta"><span class="bar" style="--p:{HERO["p"]}%" aria-hidden="true"></span><span>{HERO["left"]}</span></div>'
            f'<div class="acts"><button class="btn big" aria-label="Resume {HERO["aria"]}">{ic("play_arrow", "fill")}Resume</button>'
            f'<button class="icon-btn" aria-label="More options for {HERO["title"]}">{ic("more_vert")}</button></div></div></section>')
    cont = section("Continue watching", "8", rail([wide_card(c) for c in CONTINUE], "Continue watching"))
    libs = section("Your libraries", "4", rail([lib_card(*l) for l in LIBS], "Your libraries"))
    return screen(hero + cont + libs + common_sections(desk) + EDIT, desk)


# ---------------------------------------------------------------- B · Spotlight
def b_screen(desk=False):
    hero = (f'<section class="hb-hero" aria-label="Pick up where you left off"><div class="artbox">{art(HERO["art"])}</div><div class="in">'
            f'<span class="hb-kicker">{ic("history")}Continue watching</span>'
            f'<h2><a href="#" aria-label="{HERO["title"]}, open details">{HERO["title"]}</a></h2>'
            f'<p class="meta">{HERO["series"]} · {HERO["code"]}</p>'
            f'<div class="prog"><span class="bar thick" style="--p:{HERO["p"]}%" aria-hidden="true"></span><span>{HERO["left"]}</span></div>'
            f'<div class="acts"><button class="btn huge" aria-label="Resume {HERO["aria"]}">{ic("play_arrow", "fill")}Resume</button>'
            f'<button class="icon-btn tonal xl" aria-label="More options for {HERO["title"]}">{ic("more_vert")}</button></div></div></section>')
    cont = section("Continue watching", "8", rail([wide_card(c) for c in CONTINUE], "Continue watching"))
    libs_items = "".join(f'<li><a class="hb-lib" href="#" aria-label="{n}, {c}"><span class="em">{ic(i)}</span><span class="t"><span class="truncate">{n}</span><small>{c}</small></span></a></li>'
                         for n, i, c, _ in LIBS)
    libs = (f'<section class="hm-sec" aria-labelledby="h-libs">{head("Your libraries", "4", scroll=False).replace("<h2>", "<h2 id=\"h-libs\">", 1)}'
            f'<ul class="hb-libs">{libs_items}</ul></section>')
    return screen(hero + cont + libs + common_sections(desk) + EDIT, desk, top_in_body=True)


# ---------------------------------------------------------------- C · Index
def c_row(c, state=""):
    prog = f'<span class="bar" style="--p:{c["p"]}%" aria-hidden="true"></span>' if c.get("p") else ""
    badge = f'<span class="badge-art left new">{ic("skip_next")}Next up</span>' if c.get("p") is None and "Next up" in c["m"] else ""
    aria = f'{c["k"].replace(" · ", ", ")}, {c["t"]}, {c["m"]}'
    return (f'<li class="hc-row"><a class="hit {state}" href="#" aria-label="{aria}"><span class="thumb">{art(c["art"])}{badge}</span>'
            f'<span class="txt"><span class="mc-k truncate">{c["k"]}</span><span class="mc-t clamp-2">{c["t"]}</span><span class="mc-m">{c["m"]}</span>{prog}</span></a>'
            f'<button class="icon-btn" aria-label="More options for {c["t"]}">{ic("more_vert")}</button></li>')


def c_screen(desk=False):
    hero = (f'<section class="hc-hero" aria-label="Pick up where you left off"><div class="top"><span class="thumb">{art(HERO["art"])}</span>'
            f'<div class="txt"><span class="k">Continue watching</span><h2><a href="#" aria-label="{HERO["title"]}, open details">{HERO["title"]}</a></h2>'
            f'<span class="meta">{HERO["series"]} · {HERO["code"]} · {HERO["left"]}</span><span class="bar" style="--p:{HERO["p"]}%" aria-hidden="true"></span></div></div>'
            f'<div class="acts"><button class="btn big" aria-label="Resume {HERO["aria"]}">{ic("play_arrow", "fill")}Resume</button>'
            f'<button class="icon-btn tonal" aria-label="More options for {HERO["title"]}">{ic("more_vert")}</button></div></section>')
    rows = CONTINUE[1:5] if desk else CONTINUE[1:4]
    cont = (f'<section class="hm-sec" aria-labelledby="h-cw">{head("Continue watching", "8", scroll=False).replace("<h2>", "<h2 id=\"h-cw\">", 1)}'
            f'<ul class="hc-list">{"".join(c_row(c) for c in rows)}</ul>'
            + ("" if desk else f'<a class="btn secondary hc-all" href="#">See all 8 in Continue watching</a>') + '</section>')
    tiles = "".join(f'<li><a class="hc-lib" href="#" aria-label="{n}, {c}"><span class="em">{ic(i)}</span><span class="t"><span class="truncate">{n}</span><small>{c}</small></span></a></li>'
                    for n, i, c, _ in LIBS)
    libs = (f'<section class="hm-sec" aria-labelledby="h-libs">{head("Your libraries", "4", see_all=False, scroll=False).replace("<h2>", "<h2 id=\"h-libs\">", 1)}'
            f'<ul class="hc-libs">{tiles}</ul></section>')
    top = f'<div class="hc-top">{hero}{cont}</div>' if desk else hero + cont
    return screen(top + libs + common_sections(desk) + EDIT, desk)


def screen(body, desk, top_in_body=False):
    return f'<div class="hm{" desk" if desk else ""}">{topbar()}<main class="hm-body">{body}</main>{nav(desk)}</div>'


SCREENS = {"a": a_screen, "b": b_screen, "c": c_screen}


def frame(viewport, inner, theme=None, zoom=""):
    panel = f' data-theme-panel="{theme}"' if theme else ""
    return f'<div class="v-frame {zoom}" data-viewport="{viewport}"{panel}><div class="app">{inner}</div></div>'


def cell(label, html):
    return f'<div class="px-cell"><p class="v-label">{label}</p>{html}</div>'


# ---------------------------------------------------------------- components
def specimen(opt):
    c0 = CONTINUE[0]
    cards = {
        "a": rail([wide_card(c0), wide_card(c0, "is-focus"), wide_card(CONTINUE[2])], "Card states"),
        "b": rail([wide_card(c0), wide_card(c0, "is-hover"), wide_card(CONTINUE[2])], "Card states"),
        "c": f'<ul class="hc-list" style="padding:0">{c_row(c0)}{c_row(c0, "is-focus")}{c_row(CONTINUE[2])}</ul>',
    }[opt]
    posters = rail([poster("Low Tide", "2024", "sea", "new"), poster("Kestrel", "2023", "frost", "watched", "is-focus"),
                    poster("Ember Season", "2022", "ember", "dl"), poster("A Map of Small Hours: The Director’s Cut", "2024", "none")], "Poster states")
    libs = {
        "a": rail([lib_card(*LIBS[0]), lib_card(*LIBS[1], state="is-focus")], "Library states"),
        "b": '<ul class="hb-libs" style="padding:0">' + "".join(
            f'<li><a class="hb-lib{" is-focus" if k else ""}" href="#"><span class="em">{ic(i)}</span><span class="t"><span class="truncate">{n}</span><small>{c}</small></span></a></li>'
            for k, (n, i, c, _) in enumerate(LIBS[:2])) + "</ul>",
        "c": '<ul class="hc-libs" style="padding:0">' + "".join(
            f'<li><a class="hc-lib{" is-focus" if k else ""}" href="#"><span class="em">{ic(i)}</span><span class="t"><span class="truncate">{n}</span><small>{c}</small></span></a></li>'
            for k, (n, i, c, _) in enumerate(LIBS[:2])) + "</ul>",
    }[opt]
    chips = "".join(src_chip(k) for k in ("ok", "warn", "off", "local"))
    menu = ('<div class="menu" role="menu" aria-label="The Long Way Round">'
            f'<button class="menu-item" role="menuitem">{ic("check_circle")}<span class="text"><span>Mark as watched</span></span></button>'
            f'<button class="menu-item is-hover" role="menuitem">{ic("visibility_off")}<span class="text"><span>Remove from Continue watching</span></span></button>'
            f'<button class="menu-item" role="menuitem">{ic("tv")}<span class="text"><span>Go to Northbound</span></span></button>'
            f'<button class="menu-item" role="menuitem">{ic("favorite")}<span class="text"><span>Add to favorites</span></span></button>'
            f'<button class="menu-item" role="menuitem">{ic("download")}<span class="text"><span>Download</span></span></button></div>')
    edit = ('<div class="stackline" style="width:100%">'
            f'<div class="edit-row"><span class="text"><span>Continue watching</span><small>Shown · 1st</small></span><button class="icon-btn" disabled aria-label="Move Continue watching up">{ic("arrow_upward")}</button><button class="icon-btn" aria-label="Move Continue watching down">{ic("arrow_downward")}</button><label class="switch"><input type="checkbox" role="switch" checked aria-label="Show Continue watching"></label></div>'
            f'<div class="edit-row"><span class="text"><span>Favorites</span><small>Hidden</small></span><button class="icon-btn" aria-label="Move Favorites up">{ic("arrow_upward")}</button><button class="icon-btn" aria-label="Move Favorites down">{ic("arrow_downward")}</button><label class="switch"><input type="checkbox" role="switch" aria-label="Show Favorites"></label></div></div>')
    callout = (f'<div class="callout info">{ic("cloud_off")}<div class="body"><strong>You’re offline</strong>Showing the 2 titles on this device. Home updates when Harbour is back.</div>'
               '<button class="btn text">Retry</button></div>')
    snack = '<div class="snackbar-host"><div class="snackbar" role="status"><span class="msg">Marked “The Long Way Round” as watched</span><button class="btn text">Undo</button></div></div>'
    hero_btn = {"a": f'<button class="btn big">{ic("play_arrow", "fill")}Resume</button>',
                "b": f'<button class="btn huge">{ic("play_arrow", "fill")}Resume</button>',
                "c": f'<button class="btn big">{ic("play_arrow", "fill")}Resume</button>'}[opt]
    return f'''<div class="app sp">
<figure class="wide"><div class="hm"><div class="hm-sec">{head("Continue watching", "8")}{cards}</div></div><figcaption>Continue card · default, {"focus (3 px ring around the art)" if opt != "b" else "hover / focus (corner shape morphs)"}, Next up. The whole card is one target; ⋮ is a separate 48 px button. Screen readers hear “Northbound, S2, E5, The Long Way Round, 23 min left”.</figcaption></figure>
<figure class="wide"><div class="hm">{posters}</div><figcaption>Poster · New, watched + focus, downloaded, missing art with a long title (two lines, then ellipsis; full title in the label). Badges pair an icon with text, not colour alone.</figcaption></figure>
<figure><div class="hm">{libs}</div><figcaption>Library entry · default, focus.</figcaption></figure>
<figure><div class="line">{chips}</div><figcaption>Source chip · connected, reconnecting, offline, this device. Icon + words carry the status. 48 px target.</figcaption></figure>
<figure><div class="line">{hero_btn}<button class="btn outline">{ic("tune")}Edit Home</button><a class="btn text" href="#">See all{ic("chevron_right")}</a></div><figcaption>Resume (one primary per screen), outline, text.</figcaption></figure>
<figure><div class="hm">{nav()}</div><figcaption>Bottom bar · every tab has an icon and a label; current tab = indicator + filled icon + bold label. 56 px tabs, 64 px account button.</figcaption></figure>
<figure><div class="rel">{menu}</div><figcaption>Card menu · ⋮, long-press or right-click. Bottom sheet on phone, menu on desktop.</figcaption></figure>
<figure><div class="hm">{edit}</div><figcaption>Edit Home rows · switch + Move up / Move down buttons (no drag-only reordering).</figcaption></figure>
<figure>{callout}<figcaption>Offline (T4, inline under the top bar).</figcaption></figure>
<figure>{snack}<figcaption>Undo after a card action (T3).</figcaption></figure>
</div>'''


# ---------------------------------------------------------------- page
def option_section(key):
    o = OPTIONS[key]
    s = SCREENS[key]
    return f'''<section class="v-option opt-{key}" data-tokens="opt-{key}" id="opt-{key}">
<header><h2>{key.upper()} · {o["name"]}</h2><p>{o["lead"]}</p><p>{o["trade"]}</p><p class="v-label">{o["mood"]}</p></header>
<div class="v-section"><h2>Phone</h2><p>Scroll inside the frames to see every section down to Edit Home.</p>
<div class="px-row">{cell("Phone · light", frame("phone", s(), "light"))}{cell("Phone · dark", frame("phone", s(), "dark"))}</div></div>
<div class="v-section"><h2>Desktop</h2><p>Same sections; shelves get scroll buttons next to See all for mouse and keyboard.</p>
<div class="px-row">{cell("Desktop · dark · 1280 × 800", frame("desktop", s(desk=True), "dark", "z60"))}{cell("Desktop · light", frame("desktop", s(desk=True), "light", "z60"))}</div></div>
<div class="v-section"><h2>Components</h2>{specimen(key)}</div>
</section>'''


def compare():
    cols = "".join(f'<div class="px-cell opt-{k}" data-tokens="opt-{k}"><p class="v-label">{k.upper()} · {o["name"]}</p>{frame("phone", SCREENS[k]())}</div>'
                   for k, o in OPTIONS.items())
    return f'<section class="v-section" id="compare"><h2>Side by side</h2><p>Same library, same moment, three layouts. Follows the theme toggle.</p><div class="px-row">{cols}</div></section>'


INTRO = '''<section class="v-section"><h2>Round 1 · three layouts for Home</h2>
<p>Palette, shapes and the system font are fixed by the Marquee direction, so these differ in layout and card treatment. All three share: a Resume hero (one primary action), the source chip in the top bar, a count + See all on every section, a ⋮ on every card, Edit Home at the end, and the fixed bottom bar (icon + label on every tab).</p>
<div class="px-scroll"><table class="px-table"><thead><tr><th></th><th>A · Shelf</th><th>B · Spotlight</th><th>C · Index</th></tr></thead><tbody>
<tr><th>Hero</th><td>Art card, text on a dark scrim, 56 px Resume</td><td>Full-bleed art fading into the page, display title below, 64 px full-width Resume</td><td>Tonal panel with a thumbnail; no text on art</td></tr>
<tr><th>Continue watching</th><td>Shelf of 16:9 cards</td><td>Shelf of larger 16:9 cards</td><td>Vertical list (3 on phone, 4 in two columns on desktop) + See all</td></tr>
<tr><th>Libraries</th><td>Shelf of art cards (as today)</td><td>2 × 2 grid of quick buttons</td><td>2 × 2 grid of large tiles</td></tr>
<tr><th>Sideways scrolling on phone</th><td>Every section</td><td>All but libraries</td><td>Only New in / Favorites / Downloaded</td></tr>
<tr><th>Distance from today</th><td>Smallest</td><td>Medium</td><td>Largest</td></tr>
</tbody></table></div></section>'''

FOUNDATIONS = '''<section class="v-section opt-a" data-tokens="opt-a" id="foundations"><h2>Shared foundations</h2>
<p>Identical in all three: the Marquee palette, plus on-scrim roles for text over artwork and three extra artwork hues (stand-ins only; real artwork is images).</p>
<div data-render="roles"></div><h2 style="margin-top:32px">Contrast (WCAG 2.2 AA)</h2><div data-render="contrast"></div>
<h2 style="margin-top:32px">Type</h2><div class="v-card" data-render="type"></div>
<h2 style="margin-top:32px">Icons</h2><div data-render="icons" data-names="home,video_library,search,refresh,cloud_done,sync,cloud_off,smartphone,play_arrow,more_vert,chevron_right,skip_next,download_done,tune,visibility_off,favorite"></div></section>'''


def page():
    tokens = "\n".join(f'<!-- tokens: ../option-{k}.tokens.json scope=.opt-{k} id=opt-{k} -->' for k in OPTIONS)
    body = INTRO + "".join(option_section(k) for k in OPTIONS) + compare() + FOUNDATIONS
    return f'''<title>EmbyEx Home Directions</title>
{tokens}
<style><!-- include: viewer.css --></style>
<style><!-- include: components.css --></style>
<style><!-- include: options.css --></style>
<script>window.DESIGN = {{ app: "EmbyEx Home · Round 1", mode: "options", themes: ["light", "dark"] }};</script>
<div class="v-options">
{body}
</div>
<script><!-- include: viewer.js --></script>
'''


def main():
    for key, o in OPTIONS.items():
        data = make_tokens(f'Marquee · {o["name"]}', o["rationale"], o["sizes"])
        (OUT / f"option-{key}.tokens.json").write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n")
    (OUT / "src" / "index.html").write_text(page())
    print("wrote tokens and", OUT / "src" / "index.html")


if __name__ == "__main__":
    main()
